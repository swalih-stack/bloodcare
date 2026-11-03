package com.bloodcare.db;

import com.bloodcare.model.BloodBank;
import com.bloodcare.model.BloodGroups;
import com.bloodcare.model.Location;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** BankDao - the blood_banks table. Deleting a bank keeps its stock rows (bank becomes "Direct"). */
public final class BankDao {
    private BankDao() {}

    public static void add(BloodBank bank) throws SQLException {
        Db.update("INSERT INTO blood_banks (name, city, phone, address) VALUES (?,?,?,?)", bank.getName(),
                bank.getLocation().getDistrict(), bank.getPhone(), bank.getLocation().getPlace());
    }

    public static void delete(int id) throws SQLException {
        Db.update("DELETE FROM blood_banks WHERE id = ?", id);
    }

    /** Rows: id, name, district, phone, address, units in stock. */
    public static List<Object[]> all() throws SQLException {
        return Db.queryRows("SELECT b.id, b.name, b.city, b.phone, b.address, "
                + "COALESCE((SELECT SUM(i.units) FROM blood_inventory i WHERE i.bank_id = b.id),0) "
                + "FROM blood_banks b ORDER BY b.id");
    }

    /** Banks with a positive quantity of any group compatible with the recipient. */
    public static List<Object[]> withCompatibleBlood(String recipientGroup) throws SQLException {
        String[] groups = BloodGroups.compatibleDonorGroups(recipientGroup);
        if (groups.length == 0) return List.of();
        String marks = String.join(",", java.util.Collections.nCopies(groups.length, "?"));
        return Db.queryRows("SELECT b.name, i.blood_group, b.city, b.phone, b.address, SUM(i.units) "
                + "FROM blood_banks b JOIN blood_inventory i ON i.bank_id = b.id "
                + "WHERE i.blood_group IN (" + marks + ") GROUP BY b.id, b.name, i.blood_group, b.city, b.phone, b.address "
                + "HAVING SUM(i.units) > 0 ORDER BY SUM(i.units) DESC, b.name", (Object[]) groups);
    }

    /** For drop-down lists; BloodBank.NONE comes first as the required-choice placeholder. */
    public static List<BloodBank> items() throws SQLException {
        List<BloodBank> list = new ArrayList<>();
        list.add(BloodBank.NONE);
        for (Object[] r : Db.queryRows("SELECT id, name, city, address, phone FROM blood_banks ORDER BY name")) {
            list.add(new BloodBank((Integer) r[0], (String) r[1], Location.restore((String) r[2], (String) r[3]), (String) r[4]));
        }
        return list;
    }
}
