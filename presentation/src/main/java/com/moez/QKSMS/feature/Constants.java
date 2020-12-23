package com.moez.QKSMS.feature;

import com.moez.QKSMS.model.Conversation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class Constants {

    public static List<Conversation> conversationList = new ArrayList<>();

    public static List<Conversation> getModelList(RealmResults<Conversation> conversations) {
        List<Conversation> list = new ArrayList<>();
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            conversations = realm
                    .where(Conversation.class)
                    .findAll();
            list.addAll(realm.copyFromRealm(conversations));
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
        if (conversationList.size() > 0) {
            conversationList.clear();
        }
        conversationList.addAll(list);
        return list;
    }


    public static boolean checkTwoDay(long dateStart, long dateNext) {
        long difference = 0;
//        Long mDate = java.lang.System.currentTimeMillis();

        if (dateStart > dateNext) {
            difference = dateStart - dateNext;
            final long seconds = difference / 1000;
            final long minutes = seconds / 60;
            final long hours = minutes / 60;
            final long days = hours / 24;

            // 48 * 60 * 60
            if (seconds < 0) {
                return false;
            } else if (seconds > 84600 && seconds < 172800) {
                return true;
            }
        }
        return false;
    }


    public static String convertStringDate(long date) {
//        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(date);
        return strDate;
    }


    public static String getDisplayableTime(long dateNext, long dateStart) {

        return convertStringDate(dateNext);

//        long difference = 0;
//        Long mDate = 0L;
//        String dateString = "";
//        if (dateStart == 0) {
//            mDate = java.lang.System.currentTimeMillis();
//        } else {
//            mDate = dateStart;
//        }
//        if (mDate > dateNext) {
//            difference = mDate - dateNext;
//
//            return convertStringDate(difference);
//
////            final long seconds = difference / 1000;
////            final long minutes = seconds / 60;
////            final long hours = minutes / 60;
////            final long days = hours / 24;
////            final long months = days / 31;
////            final long years = days / 365;
////
////            if (seconds < 0) {
////                return "not yet";
////            } else if (seconds < 60) {
////                return seconds == 1 ? "one second ago" : seconds + " seconds ago";
////            } else if (seconds < 120) {
////                return "a minute ago";
////            } else if (seconds < 2700) // 45 * 60
////            {
////                return minutes + " minutes ago";
////            } else if (seconds < 5400) // 90 * 60
////            {
////                return "an hour ago";
////            } else if (seconds < 86400) // 24 * 60 * 60
////            {
////                return hours + " hours ago";
////            } else if (seconds < 172800) // 48 * 60 * 60
////            {
////                return "yesterday";
////            } else if (seconds < 2592000) // 30 * 24 * 60 * 60
////            {
////                return days + " days ago";
////            } else if (seconds < 31104000) // 12 * 30 * 24 * 60 * 60
////            {
////                return months <= 1 ? "one month ago" : days + " months ago";
////            } else {
////                return years <= 1 ? "one year ago" : years + " years ago";
////            }
//        }
//        return null;
    }
}
