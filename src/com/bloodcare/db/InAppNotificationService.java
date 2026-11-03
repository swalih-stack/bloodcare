package com.bloodcare.db;

import com.bloodcare.model.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;

/** InAppNotificationService - implements NotificationService by saving messages in the notifications table. */
public class InAppNotificationService implements NotificationService {

    @Override
    public void notifyUser(int userId, String message) throws SQLException {
        try (Connection c = Db.get()) {
            NotificationDao.toUser(c, userId, message);
        }
    }

    @Override
    public void notifyAdmins(String message) throws SQLException {
        try (Connection c = Db.get()) {
            NotificationDao.toAdmins(c, message);
        }
    }

    @Override
    public int broadcast(String message, String bloodGroupOrNull) throws SQLException {
        return NotificationDao.broadcast(message, bloodGroupOrNull);
    }
}
