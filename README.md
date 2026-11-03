# BloodCare (Java Swing + JDBC + MySQL)

## Setup
1. Install MySQL 8 and start it.
2. Download MySQL Connector/J (the "Platform Independent" zip from dev.mysql.com/downloads/connector/j)
   and copy `mysql-connector-j-*.jar` into the `lib` folder.
3. Edit `db.properties` (user / password).
4. Run `build.bat` (compiles, then starts the app). The database `bloodcare`, its tables and the
   admin account are created automatically on first run.

Admin login: `admin@bloodcare.com` / `admin123` (change it after the demo).

## Structure
- `ui/` Swing screens: `HomePanel`, `AuthPanel`, `MainFrame` (CardLayout, one window; opens `AdminDashboard` for an `Admin`,
  `UserDashboard` for a `Donor`). `DashboardShell` + `Sidebar` (collapsible menu, header, bell, clock) is the parent of both
  dashboards. Every page extends the abstract `BasePage` (its `refresh()` reloads the page from MySQL when it is opened):
  `DataPage`, `AdminHome`, `UserHome`, `InventoryPage`, `ReportsPage`, `ProfilePage` (details + password), `RequestPage`,
  `DonatePage`, `ContactPage`, `ChatPage`. Shared pieces: `StatCard`, `StockTiles`, `RoundPanel`, `Icons`, `Ui`, `AdminActions`.
- `db/` JDBC: `Db` (connection + the helpers `queryInt`, `queryRows`, `update` that every DAO reuses), `Passwords`, and the DAOs `UserDao`, `RequestDao`, `DonationDao`, `InventoryDao`, `BankDao`,
  `NotificationDao`, `MessageDao`, `ResponseDao`, `ChatDao`; `InAppNotificationService` implements `NotificationService`.
- `model/` the domain classes (object-oriented design):
  - **Inheritance:** abstract `User` -> `Donor`, `BloodSeeker`, `Admin`; `BloodRequest` -> `EmergencyRequest`.
  - **Interfaces:** `EligibilityChecker` (-> `DonorEligibilityChecker`), `NotificationService` (-> `InAppNotificationService`).
  - **Encapsulation:** private fields + getters; objects validate themselves (`User.updateProfile`, `BloodSeeker.createRequest`,
    `Donation.offer`, `BloodRequest.moveTo` enforces the status workflow); `Location`, `BloodBank`, `BloodInventory`,
    `DonationHistory` (read-only list), `BloodGroups`, `Districts`.
  - **Polymorphism:** `getRole()/getDisplayRole()` differ per account type; `EmergencyRequest.alertMessage()` overrides the base.
- `sql/schema.sql` reference schema (9 tables); `docs/class-diagram.png` the class diagram.

## Flow
Home -> Sign In -> role ADMIN opens AdminDashboard, role USER opens UserDashboard.
- User offers to donate (with age + weight) -> admin checks it on **Eligibility Verification** (age 18-65, 50 kg+, 90 days since
  last approved donation) -> Approve adds the units to stock (one transaction), Reject does not.
- User requests blood -> admin Approve / Reject / Mark Fulfilled. Fulfilling checks stock and deducts it; closed requests can't change.
- Every status change and new request/offer creates a notification (bell + badge); admin can broadcast a message to all users or one blood group.
- Places are the 14 Kerala districts (drop-downs for registration, profile, blood request and blood banks; `model/Districts`).
  When a request is submitted, every user in the **same district** with the **same blood group** (except the requester, and
  except anyone who donated in the last 90 days) is alerted immediately with an URGENT notification, in the same transaction
  that saves the request. Those local donors can see and accept the request before the admin has approved it; donors in other
  districts see it once it is approved.
- Once the admin **approves** a request, donors with the same blood group see it under **Help Requests** and can Accept or
  Decline (one answer each, never their own request). Accepting notifies the requester and opens a per-request chat under
  **Conversations**; only those two people can read or write in it, only first names are shown, and it closes if the request is
  rejected or cancelled. The admin sees every answer on **Donor Responses**. Matching is by blood group only, not proof of
  medical suitability; the hospital and admin decide that.
- Users write to the admin from **Contact Admin**; the admin reads them on the **Messages** page (unread badge), can Reply
  (the user gets the reply as a notification) or Mark Read.
- The user Dashboard shows approved donations, units donated, last donation date and the next eligible date
  (last approved donation + 90 days, with a countdown, or "Eligible" when the wait is over).
- Blood Banks page: add/delete banks; Add Blood Units can put stock under a bank.
- Reports page shows stock vs. open demand and exports a CSV.
- The hamburger button in the header collapses the sidebar to icons.

## Viva notes
- Passwords stored as `salt:hash`, never plain text.
- All SQL uses PreparedStatement with try-with-resources.
- A user can only cancel their own PENDING request (checked in the SQL `WHERE user_id=? AND status='PENDING'`).
- New accounts are always role USER; the only way to get ADMIN is the seeded account.
