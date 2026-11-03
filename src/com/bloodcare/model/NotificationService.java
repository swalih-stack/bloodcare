package com.bloodcare.model;

import java.sql.SQLException;


public interface NotificationService {

    void notifyUser(int userId, String message) throws SQLException;

    void notifyAdmins(String message) throws SQLException;

    /** Sends to every member, or only members of one blood group (null = everyone). Returns how many got it. */
    int broadcast(String message, String bloodGroupOrNull) throws SQLException;
}
