package com.ape.backuprestore.util;

import android.content.Context;

import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.utils.BackupFilePreview;
import com.ape.backuprestore.utils.ModuleType;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by android on 18-1-19.
 */

public class RestoreUtil {
    private static final String TAG = "RestoreUtil";
    private static final int TYPES[] = new int[]{
            ModuleType.TYPE_APP,
            ModuleType.TYPE_CALENDAR,
            ModuleType.TYPE_CONTACT,
            ModuleType.TYPE_MESSAGE,
            ModuleType.TYPE_MUSIC,
            ModuleType.TYPE_PICTURE,
            ModuleType.TYPE_CALL_LOG,
    };


    public static Observable<List<PersonalItemData>> getPersonalItemDatas(final Context context) {
        return Observable.create(new ObservableOnSubscribe<List<PersonalItemData>>() {
            @Override
            public void subscribe(ObservableEmitter<List<PersonalItemData>> e) throws Exception {
                ArrayList<PersonalItemData> cities = RestoreUtil.getData(context);

                e.onNext(cities);
                e.onComplete();
            }
        });
    }

    private static ArrayList<PersonalItemData> getData(Context context) {
        ArrayList<PersonalItemData> personalItemDatas = new ArrayList<>();
        BackupFilePreview preview = BackupFilePreview.getInstance();
        if (preview.init()) {
            int modules = preview.getBackupModules(context.getApplicationContext());
            for (int type : TYPES) {
                if ((modules & type) != 0) {
                    PersonalItemData item = new PersonalItemData(type, 1);
                    personalItemDatas.add(item);
                }
            }
        }
        return personalItemDatas;
    }
}
