package com.ape.backuprestore.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.ape.backuprestore.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static final String CLASS_TAG = Logger.LOG_TAG + "/FileUtils";
    static final int MD5_MASK = 0xff;
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

    /**
     * @param bytes
     * @param context
     * @return
     */
    public static String getDisplaySize(long bytes, Context context) {
        String displaySize = context.getString(R.string.unknown);
        long iKb = bytes2KB(bytes);
        if (iKb == 0 && bytes >= 0) {
            // display "less than 1KB"
            displaySize = context.getString(R.string.less_1K);
        } else if (iKb >= 1024) {
            // diplay MB
            double iMb = ((double) iKb) / 1024;
            iMb = round(iMb, 2, BigDecimal.ROUND_UP);
            StringBuilder builder = new StringBuilder(new Double(iMb).toString());
            builder.append("MB");
            displaySize = builder.toString();
        } else {
            // display KB
            StringBuilder builder = new StringBuilder(new Long(iKb).toString());
            builder.append("KB");
            displaySize = builder.toString();
        }
        Logger.d(CLASS_TAG, "getDisplaySize:" + displaySize);
        return displaySize;
    }

    /**
     * @param filePath create file
     * @return file
     */
    public static File createFile(String filePath) {
        File file = null;
        File tmpFile = new File(filePath);
        if (createFileorFolder(tmpFile)) {
            file = tmpFile;
        }
        return file;
    }

    /**
     * @param file create files
     * @return success
     */
    public static boolean createFileorFolder(File file) {
        boolean success = true;
        if (file != null) {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            try {
                if (file.isFile()) {
                    success = file.createNewFile();
                } else {
                    success = file.mkdirs();
                }
            } catch (IOException e) {
                success = false;
                Logger.d(CLASS_TAG,
                        "createFile() failed !cause:" + e.getMessage());
                e.printStackTrace();
            }
        }
        return success;
    }

    /**
     * @param fileName see if the file exsit
     * @return nameWithoutExt
     */
    public static String getNameWithoutExt(String fileName) {
        String nameWithoutExt = fileName;
        int iExtPoint = fileName.lastIndexOf(".");
        if (iExtPoint != -1) {
            nameWithoutExt = fileName.substring(0, iExtPoint);
        }
        return nameWithoutExt;
    }

    /**
     * @param bytes
     * @return
     */
    public static long bytes2MB(long bytes) {
        return bytes2KB(bytes) / 1024;
    }

    /**
     * @param bytes
     * @return
     */
    public static long bytes2KB(long bytes) {
        return bytes / 1024;
    }

    /**
     * @param file return the filename's ext
     * @return
     */
    public static String getExt(File file) {
        if (file == null) {
            return null;
        }
        return getExt(file.getName());
    }

    /**
     * @param fileName return the filename's ext
     * @return
     */
    public static String getExt(String fileName) {
        if (fileName == null) {
            return null;
        }
        String ext = null;

        int iLastOfPoint = fileName.lastIndexOf(".");
        if (iLastOfPoint != -1) {
            ext = fileName.substring(iLastOfPoint + 1, fileName.length());
        }
        return ext;
    }

    /**
     * @param value
     * @param scale
     * @param roundingMode
     * @return
     */
    public static double round(double value, int scale, int roundingMode) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }

    /**
     * @param folderFile
     * @return
     */
    public static long computeAllFileSizeInFolder(File folderFile) {
        long size = 0;
        if (folderFile != null) {
            try {
                if (folderFile.isFile()) {
                    size = folderFile.length();
                } else if (folderFile.isDirectory()) {
                    File[] files = folderFile.listFiles();
                    for (File file : files) {
                        if (file.isDirectory()) {
                            size += computeAllFileSizeInFolder(file);
                        } else if (file.isFile()) {
                            size += file.length();
                        }
                    }
                }
            } catch (NullPointerException e) {
                size = 0;
                Logger.e(CLASS_TAG, "computeAllFileSizeInFolder: sd card is out when ");
                e.printStackTrace();
            }
        }
        return size;
    }

    /**
     * @param folderName
     * @return
     */
    public static boolean isEmptyFolder(File folderName) {
        boolean ret = true;

        if (folderName != null && folderName.exists()) {
            if (folderName.isFile()) {
                ret = false;
            } else {
                File[] files = folderName.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!isEmptyFolder(file)) {
                            ret = false;
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param file
     * @return
     */
    public static boolean deleteFileOrFolder(File file) {
        boolean result = true;
        if (!file.exists()) {
            return result;
        }
        if (file.isFile()) {
            return file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            try {
                for (File f : files) {
                    if (!deleteFileOrFolder(f)) {
                        result = false;
                    }
                }
                // Bug Fix for CR ALPS01821649
            } catch (NullPointerException e) {
                Logger.e(CLASS_TAG, "NullPointerException: sd card is error");
            }
            if (!file.delete()) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @param file
     * @param context
     * @return
     */
    public static boolean deleteFileOrFolder(File file, Context context) {
        boolean result = true;
        if (!file.exists()) {
            return result;
        }
        if (file.isFile()) {
            return file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (file.getName().equals(Constants.ModulePath.FOLDER_MUSIC)
                    || file.getName().equals(Constants.ModulePath.FOLDER_PICTURE)) {
                // delete database!
                List<String> mediaFiles = new ArrayList<String>();
                for (File tFile : files) {
                    mediaFiles.add(tFile.getAbsolutePath());
                }
                deleteFileInMediaStore(mediaFiles, context);
            }
            try {
                for (File f : files) {
                    if (!deleteFileOrFolder(f, context)) {
                        result = false;
                    }
                }
                // Bug Fix for CR ALPS01821649
            } catch (NullPointerException e) {
                Logger.e(CLASS_TAG, "NullPointerException: sd card is error");
            }
            if (!file.delete()) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @param path     the scan path
     *                 scan Path for new file or folder in MediaStore
     * @param mContext
     */
    public static void scanPathforMediaStore(String path, Context mContext) {
        Logger.d(CLASS_TAG, "scanPathforMediaStore.path =" + path);
        if (mContext != null && !TextUtils.isEmpty(path)) {
            String[] paths = {
                    path
            };
            Logger.d(CLASS_TAG, "scanPathforMediaStore,scan file .");
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        }
    }

    /**
     * @param scanPaths
     * @param mContext
     */
    public static void scanPathforMediaStore(List<String> scanPaths, Context mContext) {
        Logger.d(CLASS_TAG, "scanPathforMediaStore,scanPaths.");
        if (mContext != null && !scanPaths.isEmpty()) {
            String[] paths = new String[scanPaths.size()];
            scanPaths.toArray(paths);
            Logger.d(CLASS_TAG, "scanPathforMediaStore,scan file.");
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        }
    }

    /**
     * @param paths    the delete file or folder in MediaStore
     * @param mContext delete the record in MediaStore
     */
    public static void deleteFileInMediaStore(List<String> paths, Context mContext) {
        Logger.d(CLASS_TAG, "deleteFileInMediaStore.");
        Uri uri = MediaStore.Files.getContentUri("external");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("?");
        for (int i = 0; i < paths.size() - 1; i++) {
            whereClause.append(",?");
        }
        String where = MediaStore.Files.FileColumns.DATA + " IN(" + whereClause.toString() + ")";
        // notice that there is a blank before "IN(".
        if (mContext != null && !paths.isEmpty()) {
            ContentResolver cr = mContext.getContentResolver();
            String[] whereArgs = new String[paths.size()];
            paths.toArray(whereArgs);
            Logger.d(CLASS_TAG, "deleteFileInMediaStore,delete.");

            try {
                cr.delete(uri, where, whereArgs);
            } catch (UnsupportedOperationException e) {
                Logger.d(CLASS_TAG, "Error, database is closed!!!");
            }
        }
    }

    /**
     * @param file
     * @return
     */
    public static ArrayList<File> getAllApkFileInFolder(File file) {
        if (file == null) {
            return null;
        }
        if (!file.exists() || file.isFile()) {
            return null;
        }
        ArrayList<File> list = new ArrayList<File>();
        File[] files = file.listFiles();
        for (File f : files) {
            String ext = getExt(f);
            if (ext != null && ext.equalsIgnoreCase("apk")) {
                list.add(f);
            }
        }
        return list;
    }

    /**
     * @param files
     * @return
     */
    public static File getNewestFile(List<File> files) {
        // TODO Auto-generated method stub
        if (files == null || files.isEmpty()) {
            return null;
        }
        File newestFile = files.get(0);
        long newest = newestFile.lastModified();
        for (File file : files) {
            Logger.d(CLASS_TAG, "onStart() ---->" + file.getName() + "  lastModified = "
                    + file.lastModified());
            newestFile = (file.lastModified() > newest ? file : newestFile);
        }
        return newestFile;
    }

    /**
     * @param file
     * @return
     */
    public static String getFileMD5(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        FileInputStream fis = null;
        StringBuffer buf = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[BUFF_SIZE];
            int length = -1;

            long s = System.currentTimeMillis();
            if (fis == null || md == null) {
                return null;
            }
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            byte[] bytes = md.digest();
            if (bytes == null) {
                return null;
            }
            for (int i = 0; i < bytes.length; i++) {
                String md5s = Integer.toHexString(bytes[i] & MD5_MASK);
                if (md5s == null || buf == null) {
                    return null;
                }
                if (md5s.length() == 1) {
                    buf.append("0");
                }
                buf.append(md5s);
            }
            Logger.d(CLASS_TAG, "getFileMD5:GenMd5 success! spend the time: "
                    + (System.currentTimeMillis() - s) + "ms");
            return buf.toString();
        } catch (Exception ex) {

            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Describe <code>writeToFile</code> method here.
     *
     * @param fileName a <code>String</code> value
     * @param buf      a <code>byte</code> value
     * @throws IOException if an error occurs
     */
    public static void writeToFile(String fileName, byte[] buf) throws IOException {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param files
     * @param saveFileName
     * @throws IOException
     */
    public static void combineFiles(List<File> files, String saveFileName) throws IOException {
        if (files == null || files.isEmpty()) {
            Logger.d(CLASS_TAG, "no file need to be combined, return and do nothing");
            return;
        }

        File mFile = new File(saveFileName);
        FileChannel mFileChannel = null;
        FileChannel inFileChannel = null;
        IOException exception = null;

        try {
            if (!mFile.exists()) {
                mFile.createNewFile();
            }

            mFileChannel = new FileOutputStream(mFile).getChannel();

            for (File file : files) {
                inFileChannel = new FileInputStream(file).getChannel();

                long position = 0;
                long maxCount = inFileChannel.size();
                Logger.d(CLASS_TAG,
                        "[combineFiles] inFileChannel.size " + inFileChannel.size());

                while (position < inFileChannel.size()) {
                    long count = inFileChannel.transferTo(position, maxCount, mFileChannel);
                    if (count > 0) {
                        position += count;
                        maxCount -= count;
                    }
                    Logger.d(CLASS_TAG, "[combineFiles] count/pos is " + count + "/"
                            + position);
                }

                inFileChannel.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            exception = e;

        } finally {
            try {
                Logger.d(CLASS_TAG, "Close the two file channel begin.");
                if (mFileChannel != null && mFileChannel.isOpen()) {
                    Logger.d(CLASS_TAG, "Close mFileChannel");
                    mFileChannel.close();
                }
                if (inFileChannel != null && inFileChannel.isOpen()) {
                    Logger.d(CLASS_TAG, "Close inFileChannel");
                    inFileChannel.close();
                }
                Logger.d(CLASS_TAG, "Close the two file channel end.");
            } catch (IOException e) {
                e.printStackTrace();
                Logger.d(CLASS_TAG, "Exception when close");
                if (exception != null) {
                    exception = e;
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * @param file
     */
    public static void deleteEmptyFolder(File file) {
        // TODO Auto-generated method stub
        if (file == null || !file.isDirectory()) {
            return;
        }
        for (File subFolderFile : file.listFiles()) {
            if (subFolderFile.isDirectory() && isEmptyFolder(subFolderFile)) {
                subFolderFile.deleteOnExit();
            }
        }
    }

}
