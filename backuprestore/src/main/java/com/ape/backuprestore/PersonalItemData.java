package com.ape.backuprestore;


import com.ape.backuprestore.utils.ModuleType;

public class PersonalItemData {

    private int mType;
    private int mCount;
    private boolean mIsEnable;
    private boolean mIsSelected;

    public PersonalItemData(int type, int count) {
        mType = type;
        mCount = count;
        mIsEnable = !(count == 0);
        setDefaultSelectedValue(mType);
    }

    private void setDefaultSelectedValue(int type) {
        switch (type) {
            case ModuleType.TYPE_CALENDAR:
            case ModuleType.TYPE_CALL_LOG:
            case ModuleType.TYPE_MESSAGE:
            case ModuleType.TYPE_CONTACT:
                if (mIsEnable) {
                    mIsSelected = true;
                }
                break;
        }
    }

    public int getType() {
        return mType;
    }

    public int getCount() {
        return mCount;
    }

    public int getIconId() {
        int ret = ModuleType.TYPE_INVALID;
        switch (mType) {
            case ModuleType.TYPE_CONTACT:
                ret = R.drawable.icon_data_contact_normal;
                break;

            case ModuleType.TYPE_MESSAGE:
                ret = R.drawable.icon_data_message_normal;
                break;

            case ModuleType.TYPE_PICTURE:
                ret = R.drawable.icon_data_albums_normal;
                break;
            case ModuleType.TYPE_CALENDAR:
                ret = R.drawable.icon_data_calendar_normal;
                break;

            case ModuleType.TYPE_MUSIC:
                ret = R.drawable.icon_data_music_normal;
                break;

            case ModuleType.TYPE_CALL_LOG:
                ret = R.drawable.icon_data_calllog_normal;
                break;
            case ModuleType.TYPE_APP:
                ret = R.drawable.icon_data_app_normal;
                break;
//        case ModuleType.TYPE_BOOKMARK:
//            ret = R.drawable.ic_bookmark;
//            break;

            default:
                break;
        }
        return ret;
    }

    public int getTextId() {
        int ret = ModuleType.TYPE_INVALID;
        switch (mType) {
//        case ModuleType.TYPE_SELECT:
//            ret = R.string.selectall;
//            break;

            case ModuleType.TYPE_CONTACT:
                ret = R.string.contact_module;
                break;

            case ModuleType.TYPE_MESSAGE:
                ret = R.string.message_module;
                break;

            case ModuleType.TYPE_PICTURE:
                ret = R.string.picture_module;
                break;
            case ModuleType.TYPE_CALENDAR:
                ret = R.string.calendar_module;
                break;

            case ModuleType.TYPE_MUSIC:
                ret = R.string.music_module;
                break;

            case ModuleType.TYPE_NOTEBOOK:
                ret = R.string.notebook_module;
                break;
            case ModuleType.TYPE_APP:
                ret = R.string.app_module;
                break;
            case ModuleType.TYPE_CALL_LOG:
                ret = R.string.calllog_module;
                break;
//        case ModuleType.TYPE_BOOKMARK:
//            ret = R.string.bookmark_module;
//            break;

            default:
                break;
        }
        return ret;
    }

    public boolean isEnable() {
        return mIsEnable;
    }

    public void setEnable(boolean enable) {
        mIsEnable = enable;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }
}
