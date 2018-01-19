package com.ape.backuprestore.util;

import android.content.Context;
import android.util.Log;

import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.modules.AppBackupComposer;
import com.ape.backuprestore.modules.CalendarBackupComposer;
import com.ape.backuprestore.modules.CallLogBackupComposer;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.modules.ContactBackupComposer;
import com.ape.backuprestore.modules.MmsBackupComposer;
import com.ape.backuprestore.modules.MusicBackupComposer;
import com.ape.backuprestore.modules.NoteBookBackupComposer;
import com.ape.backuprestore.modules.PictureBackupComposer;
import com.ape.backuprestore.modules.SmsBackupComposer;
import com.ape.backuprestore.utils.ModuleType;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by android on 18-1-19.
 */

public class BackupUtil {
    private static final String TAG = "BackupUtil";
    private static final int TYPES[] = new int[]{
            ModuleType.TYPE_CONTACT,
            ModuleType.TYPE_MESSAGE,
            ModuleType.TYPE_CALL_LOG,
            ModuleType.TYPE_CALENDAR,
            ModuleType.TYPE_PICTURE,
            ModuleType.TYPE_MUSIC,
            ModuleType.TYPE_APP
    };

    public static Observable<List<PersonalItemData>> getPersonalItemDatas(final Context context) {
        return Observable.create(new ObservableOnSubscribe<List<PersonalItemData>>() {
            @Override
            public void subscribe(ObservableEmitter<List<PersonalItemData>> e) throws Exception {
                ArrayList<PersonalItemData> cities = BackupUtil.getData(context);

                e.onNext(cities);
                e.onComplete();
            }
        });
    }

    private static ArrayList<PersonalItemData> getData(Context context) {
        ArrayList<PersonalItemData> personalItemDatas = new ArrayList<>();
        for (int type : TYPES) {
            int count = 0;
            switch (type) {
                case ModuleType.TYPE_CONTACT:
                    count = getModulesCount(new ContactBackupComposer(context));
                    break;
                case ModuleType.TYPE_MESSAGE:
                    int countSMS = 0;
                    int countMMS = 0;
                    Composer smsBackupComposer = new SmsBackupComposer(context);
                    if (smsBackupComposer.init()) {
                        countSMS = smsBackupComposer.getCount();
                        smsBackupComposer.onEnd();
                    }

                    Composer mmsBackupComposer = new MmsBackupComposer(context);
                    if (mmsBackupComposer.init()) {
                        countMMS = mmsBackupComposer.getCount();
                        mmsBackupComposer.onEnd();
                    }
                    count = countSMS + countMMS;
                    Log.i(TAG, "countSMS = " + countSMS + ", countMMS = " + countMMS);
                    break;
                case ModuleType.TYPE_PICTURE:
                    count = getModulesCount(new PictureBackupComposer(context));
                    break;
                case ModuleType.TYPE_CALENDAR:
                    count = getModulesCount(new CalendarBackupComposer(context));
                    break;
                case ModuleType.TYPE_APP:
                    count = getModulesCount(new AppBackupComposer(context));
                    break;
                case ModuleType.TYPE_MUSIC:
                    count = getModulesCount(new MusicBackupComposer(context));

                    break;
                case ModuleType.TYPE_NOTEBOOK:
                    count = getModulesCount(new NoteBookBackupComposer(context));
                    break;
                case ModuleType.TYPE_CALL_LOG:
                    count = getModulesCount(new CallLogBackupComposer(context));
                    break;
                default:
                    Log.i(TAG, "Unknown module type: " + type);
                    break;
            }
            PersonalItemData item = new PersonalItemData(type, count);
            Log.i(TAG, "Add module type: " + type);
            personalItemDatas.add(item);
        }
        return personalItemDatas;
    }

    private static int getModulesCount(Composer... composers) {
        int count = 0;
        for (Composer composer : composers) {
            if (composer.init()) {
                count += composer.getCount();
                composer.onEnd();
            }
        }
        Log.i(TAG, "getModulesCount : " + count);
        return count;
    }
}
