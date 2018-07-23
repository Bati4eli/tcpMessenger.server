package server.db;

import com.sun.istack.internal.Nullable;
import org.json.JSONObject;
import source.Const;

import java.io.DataInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static server.db.SqlServer.DEVICE_STATE.*;
import static server.db.SqlServer.SQL_FILES.*;
import static server.db.SqlServer.dBook.*;
import static server.db.SqlServer.dBook.USER_ID;
import static server.db.SqlServer.dDevice.*;
import static source.Const.ANSI_COLOR.ANSI_GREEN;
import static source.Const.PrintColor;
import static source.Const.getAbsolutePath;

public class SqlServer {
    private static SqlServer sqlServer = new SqlServer();
    private SqlServer(){}
    public static SqlServer get(){return sqlServer;}


    private Connection conn;



    public enum SQL_FILES{
        SQL_CREATETABLE ("CREATE_TABLES.sql"),
        SQL_CREATEINDEX ("CREATE_INDEX.sql"),
        SQL_CREATETRIGGER("CREATE_TRIGGER.sql"),
        SQL_DEVICE_SELECT("device_select.sql"),
        SQL_SIGN_IN ("device_sign_in.sql"),
        SQL_SIGN_UP ("users_sign_up.sql"),
        SQL_ACTIVATE("device_activate.sql"),
        SQL_DEACTIVATE("device_deactivate.sql"),
        SQL_BIND ("device_bind.sql"),
//        SQL_LAST_BIND("device_last_bind.sql"),
        SQL_USERS_SELECT("users_select.sql"),
        SQL_BOOK_SELECT("book_select_.sql"),
        SQL_BOOK_INSERT("book_insert.sql"),
        SQL_BOOK_UPDATE("book_update.sql"),
        SQL_BOOK_USERS_LIST_ONLINE("book_users_list_online.sql"),
        SQL_BOOK_ITEMS("book_items.sql")
        ;
        private String sqlName ; //getAbsolutePath() + "src/server/db/sqlFiles/";
        SQL_FILES(String sqlName){this.sqlName ="/server/db/sqlFiles/" + sqlName;}
        @Override
        public String toString(){return sqlName;}
        private String readInternalFile() { // Читает файл из внутреннних ресурсов JAR
            String path = this.toString();
            DataInputStream dis = new DataInputStream(getClass().getResourceAsStream(path));
            StringBuilder strBuild = new StringBuilder();
            int ch = 0;
            try {
                while ((ch = dis.read()) != -1) {
                    strBuild.append((char) ((ch >= 0xc0 && ch <= 0xFF) ? (ch + 0x350) : ch));
                }
                dis.close();
            } catch (Exception e) {
                System.err.println("ERROR in openInternalSrc(): " + e);
            }
            return strBuild.toString();
        }
        private String setParametr(@Nullable String...args) { // автопростановка в коде sql параметров
            String sql =  readInternalFile();
            if(args.length%2!=0) {
                return null;
            } else {
                for(int i=0;i<args.length;i+=2){
                    String arg = args[i+1]==null?"null":args[i+1];
                    sql=sql.replace("@"+args[i]+"@",arg);
                }
                return sql;
            }
        }
    }//enum SQL_FILES
    public enum DEVICE_STATE{
        DS_NON,
        DS_ACTIVATED,
        DS_DELETED,
        DS_NOT_ACTIVATED
    }

    public enum dDevice{
        PHONE,
        USER_ID,
        DEVICE_ID,
        DEVICE_NAME,
        ACTIVE,
        DELETED,
        SMS_CODE,
        MINUTES_LAST_BIND,
        ATTEMPTS
    }
    public enum dBook{
        ITEMS,
        USER_ID,
        FRIEND_ID,
        FRIEND_PHONE,
        NICKNAME,
        FULLNAME,
        WAS_IN_BASE,
        IS_UPDATE,
        IS_REGISTERED,
        IS_FRIEND,
        IS_ONLINE
    }

    public void main(String[] args) {
        try {


            String sql = SQL_SIGN_IN.setParametr("PHONE","+7 925 200","DEVICE_ID",UUID.randomUUID().toString(),"SMS_CODE"," Herovui test");
            System.out.println(sql);


        } catch (Exception ignored){

        }
    }
    public void connectDB() throws Exception    {// --------ПОДКЛЮЧЕНИЕ К БАЗЕ ДАННЫХ--------
        conn = null;
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + getAbsolutePath() +"/SERVERDB.s3db");

        Print(ANSI_GREEN,"База Подключена! " );

        if (notTableExists("users") || notTableExists("DEVICES")){
            createDB(); //Создаем необходимые сущности
        }
    }
    private void executeAll(Statement statmt,String sql)throws Exception{ //Разбивает запрос
            String[] sqls = sql.split(";;");    //разделение по двойному знаку, ибо в тригерах внутри конскрукции есть точка с запятой
            for (String s : sqls) {
                if (!s.equals("\r\n")) statmt.execute(s);
            }
    }
    public void TRUNCATE() throws Exception{
        try(Statement stat=conn.createStatement())
        {
            String sql = "DELETE FROM `users`; " +"REINDEX `users`; " +"VACUUM; ";
            executeAll(stat,sql);
        } catch (Exception e){
            System.out.println("Error on TRUNCATE: " + e);
        }
    }
    private void createDB() {
        try(Statement statmt =conn.createStatement()) {
            executeAll(statmt, SQL_CREATETABLE.readInternalFile()); Print(ANSI_GREEN, "ТАБЛИЦЫ СОЗДАНЫ");
            executeAll(statmt, SQL_CREATEINDEX.readInternalFile()); Print(ANSI_GREEN, "ИНДЕКСЫ СОЗДАНЫ");
            executeAll(statmt,SQL_CREATETRIGGER.readInternalFile());Print(ANSI_GREEN, "ТРИГГЕРЫ СОЗДАНЫ");
        } catch (Exception e){
            System.out.println("Error SqlServer: " + e );
        }
    }
    private boolean invalidCharsPhone(String phone){    // допустимы +7*** *** **** или ники с @***..
        return !Pattern.compile("^\\+7\\d{10}$").matcher(phone).find(); ///"(^\\+7\\d{10}$)|(^@[a-zA-Z0-9_]+$)"
    }
    private String cleanPhoneNumber(String phone){
        ///--- очистка от форматирования
        Pattern p = Pattern.compile("(^\\+7)|(\\d)");
        Matcher m = p.matcher(phone);
        StringBuilder builder = new StringBuilder();
        while(m.find()) {
            builder.append(phone.substring(m.start(), m.end()));
        }
        phone = builder.toString();
        ///---
        if( Pattern.compile("^8\\d{10}$").matcher(phone).find() ){
            phone = "+7" + phone.substring(1,phone.length());
            return phone;
        } else {
            if( Pattern.compile("^\\+7\\d{10}$").matcher(phone).find() ){
                return phone;
            }else {
                return null;
            }
        }
    }

//    public int getAmountAttempts(String phone, String device_id ) {
//        String sql = SQL_DEVICE_SELECT.setParametr(PHONE.name(),phone,    DEVICE_ID.toString(),device_id);
//        try(
//                Statement statmt =conn.createStatement() ;
//                ResultSet resSet =statmt.executeQuery(sql)
//        )
//        {
//            return resSet.getInt(ATTEMPTS.name());
//        } catch (Exception e){
//            System.out.println("getAmountAttempts: " + e.toString());
//            return 0;
//        }
//    }
//    public int getLastBind(String phone){
//        String sql = SQL_LAST_BIND.setParametr(PHONE.toString(),phone);
//        try(
//                Statement statmt =conn.createStatement() ;
//                ResultSet resSet =statmt.executeQuery(sql)
//        )
//        {
//            return resSet.getInt(DATE_BIND.toString());
//        } catch (Exception e){
//            return 1000;
//        }
//    }

    public DeviceInfo activate(String phone, String device_id , String smsCode) {
        Integer userid = getUserInfo(phone).userid;
        String sql = SQL_ACTIVATE.setParametr(
                USER_ID.name(),userid.toString(),
                DEVICE_ID.toString(),device_id ,
                SMS_CODE.toString(), smsCode
        );
        try(
                Statement statmt =conn.createStatement()
        )
        {
            statmt.execute(sql);
            DeviceInfo di = getDeviceInfo(phone,device_id);
            return di;
        } catch (Exception e){
            return null;
        }
    }

    public boolean bind(String phone, String device_id, String device_name , String smsCode) {        // привязка
        if(sign_up(phone) ) {       // Обязательно внести номер в основную таблицу, только потом регистрировать девайс
            Integer userid = getUserInfo(phone).userid;
            String sql = SQL_BIND.setParametr(
                    USER_ID.name(),userid.toString(),
                    DEVICE_ID.name(),device_id,
                    DEVICE_NAME.name(),device_name,
                    SMS_CODE.name(),smsCode
            );
            try(
                    Statement statmt =conn.createStatement()
            ) {
                statmt.execute(sql);   // вносим в таблицу девайс, номер, имя девайса, смс код // теперь хэндлер может отправить смс клиенту
                return true;
            } catch (Exception e){
                System.out.println("Error SqlServer BIND: " + e);
                return false;
            }
        } else {
            System.out.println("Error SqlServer BIND: SQL could not register an account");
            return false;
        }
    }
    public JSONObject sync_book(Integer userid, JSONObject book) {
        String union = " UNION \n";
        StringBuilder sqlItems =new StringBuilder(); //


        String number,fullname, tmpNum;
        Iterator phones = book.keys();
        while (phones.hasNext()) {  //трансформируем присланный клиентом список в SQL запрос
            tmpNum = (String)phones.next();
            number = tmpNum;
            fullname=book.getJSONObject(number).get(FULLNAME.name()).toString();
            if(invalidCharsPhone(number)){ // если при чтении данных от клиента находятся неверные номера, то удаляем их или реформатим
                number = cleanPhoneNumber(number);
            }
            if(number!=null) { // если телефон не был потерт , то заносим его в sql скрипт для проверки
                sqlItems.append(
                        SQL_BOOK_ITEMS.setParametr(
                            USER_ID.name(), userid.toString(),
                            FRIEND_PHONE.name(), number,
                            FRIEND_ID.name(), "null",
                            FULLNAME.name(), fullname
                        )
                );
                if(phones.hasNext()) sqlItems.append(union);
            } else{
                System.out.println("Number is deleted from SYNC: " + tmpNum + " - " + fullname );
            }
        }// while
        sqlItems =cleanStrBld(sqlItems,union);  // убираем последний UNION

        book= new JSONObject(); // Очищаем и заполняем заново
        //------------- ВЫПОЛНЯЕМ
        try(
            Statement statmt =conn.createStatement() ;
            ResultSet resSet =statmt.executeQuery(
                    SQL_BOOK_SELECT.setParametr(ITEMS.name(),sqlItems.toString() , USER_ID.name(), userid.toString() )
            );
        )
        {
            boolean mustInsert =false;
            sqlItems.setLength(0);

            ///------------------------------ Собираем информацию из базы и кладем в json
            while(resSet.next()) { // while(resSet.next())
                number = resSet.getString(FRIEND_PHONE.name());
                fullname=coalesce(resSet.getString(FULLNAME.name()),"") ;
                boolean b1 = resSet.getInt(IS_REGISTERED.name()) == 1;
                boolean b2 = resSet.getInt(IS_FRIEND.name()) == 1;
                boolean b3 = resSet.getString(WAS_IN_BASE.name()).equals("CLIENT_HAVE");
                boolean b4 = resSet.getInt(IS_UPDATE.name()) == 1;
                String nick = coalesce(resSet.getString(NICKNAME.name()),"") ;
                Integer friendID = resSet.getInt(FRIEND_ID.name());
                if(b3){ /// собираем в UNION запрос всех кого нужно вставить, иначе построчная вставка занимает много времени
                    sqlItems.append(
                            SQL_BOOK_ITEMS.setParametr(
                                USER_ID.name(), userid.toString(),
                                FRIEND_PHONE.name(), number,
                                FRIEND_ID.name(), b1?friendID.toString():"null",
                                FULLNAME.name(), fullname
                            )
                    );
                    sqlItems.append(union);
                    mustInsert= true;
                }
                if(b4) book_update_name(userid,fullname,number); //апдейтим сразу построчно (нечастая процедура и база не умеет апдейтить из запроса)

                if(b1) {// экономим время на вставке только тех кто зарегистрирован??
                    book.put(
                            Integer.toString(friendID), new JSONObject()
                                    .put(FRIEND_PHONE.name(),number)
                                    .put(NICKNAME.name(), nick)
                                    .put(FULLNAME.name(), fullname)
                                    .put(IS_REGISTERED.name(), b1)
                                    .put(IS_FRIEND.name(), b2)
                                    .put(WAS_IN_BASE.name(), resSet.getString(WAS_IN_BASE.name()))
                                    .put(IS_UPDATE.name(), b4)
                    );
                }
            } // while(resSet.next())
            if(mustInsert) {
                sqlItems = cleanStrBld(sqlItems, union);  // убираем последний UNION
                book_insert_new(sqlItems);
            }
            return book;
        }  catch (Exception e){
            System.out.println("Error on SQLSERVER on sync_book: "+e);
            return null;
        }
    }
    private StringBuilder cleanStrBld(StringBuilder sqlItems, String lastStr){
        if(sqlItems.substring(sqlItems.length() - lastStr.length() ).equals(lastStr)){ // убираем union
            sqlItems.setLength(sqlItems.length() - lastStr.length());
        }
        return sqlItems;
    }
    private String coalesce(String val1, String val2){
        return val1!=null?val1:val2;
    }
    private void book_update_name(Integer userid, String fullname, String friendPhone) {
        try (Statement statmt =conn.createStatement())
        {
            statmt.execute(
                    SQL_BOOK_UPDATE.setParametr(
                            USER_ID.name(),userid.toString(),
                            FRIEND_PHONE.name(),friendPhone,
                            FULLNAME.name(),fullname
                    )
            );
        }catch (Exception e){
            System.out.println("Error on SQLSERVER on book_update_names: " + e  );
        }
    }
    private void book_insert_new(StringBuilder sqlItems) {
        try (Statement statmt =conn.createStatement())
        {
            statmt.execute(
                    SQL_BOOK_INSERT.setParametr( ITEMS.name(),sqlItems.toString() )
            );
        }catch (Exception e){
            System.out.println("Error on SQLSERVER on book_insert_new: "+e);
        }finally {
            sqlItems.setLength(0);
        }
    }
    public ArrayList<Integer> getFriendList(Integer userid) {
        //возвращает список тех у кого этот ID в списке, а не список ID его друзей!
        String sql = SQL_BOOK_USERS_LIST_ONLINE.setParametr(USER_ID.name(),userid.toString());
        try(
                ResultSet resSet= conn.createStatement().executeQuery(sql);
        ){
            ArrayList<Integer> list = new ArrayList<>();
            while(resSet.next()){
                list.add(resSet.getInt(0));
            }
            return list;
        } catch (Exception e){
            return null;
        }
    }

    public UserInfo getUserInfo(String phone){
        String sql = SQL_USERS_SELECT.setParametr(PHONE.name(),phone );
        try(
                Statement statmt =conn.createStatement() ;
                ResultSet resSet =statmt.executeQuery(sql)
        )
        {
            UserInfo ui = new UserInfo();
            ui.userid   = resSet.getInt(USER_ID.name());
            ui.phone    = resSet.getString(PHONE.name());
            ui.fullname = resSet.getString(FULLNAME.name());
            ui.nick     = resSet.getString(NICKNAME.name());
            ui.lastBind = resSet.getInt(MINUTES_LAST_BIND.name());
            return ui;
        } catch (Exception e){
            UserInfo ui = new UserInfo();
            ui.lastBind =1000;
            return ui;
        }
    }
    public DeviceInfo getDeviceInfo(String phone, String device_id) {
        String sql = SQL_DEVICE_SELECT.setParametr(PHONE.name(), phone, DEVICE_ID.toString(), device_id);
        try (
                Statement statmt = conn.createStatement();
                ResultSet resSet = statmt.executeQuery(sql)
        ) {
            DeviceInfo di   = new DeviceInfo();
            di.userId       = resSet.getInt(USER_ID.name());
            di.deviceId     = resSet.getString(DEVICE_ID.name());
            di.deviceName   = resSet.getString(DEVICE_NAME.name());
            di.active       = resSet.getInt(ACTIVE.name())==1;
            di.deleted      = resSet.getInt(DELETED.name())==1;
            di.attempts     = resSet.getInt(ATTEMPTS.name());
            di.deviceState  = getStateDevice(di);
            return di;
        } catch (Exception e) {
            DeviceInfo di   = new DeviceInfo();
            di.deviceState=DS_NON;
            return di;
        }
    }
    private DEVICE_STATE getStateDevice(DeviceInfo deviceInfo ) {
        try{
            if( deviceInfo.deleted){
                return DS_DELETED;
            } else if( deviceInfo.active){
                return DS_ACTIVATED;
            } else {
                return DS_NOT_ACTIVATED;
            }
        } catch (Exception e){
            System.out.println("getStateDevice: " + e.toString());
            return DS_NON;
        }
    }

    public void sign_out(int userid, String device_id){
        String sql = SQL_DEACTIVATE.setParametr(USER_ID.name(), Integer.toString(userid) ,    DEVICE_ID.name(),device_id );
        try(
                Statement statmt =conn.createStatement()
        )
        {
            statmt.execute(sql);
        } catch (Exception ignored){
        }
    }
    private boolean sign_up(String phone){
        // Автоматическая функция заполнение основной таблицы с зарегестрированными телефонами
        // ------ Регистрация телефона -- по умолчанию закладываем только номер
        if(!phoneExists(phone)) {
            String sql = SQL_SIGN_UP.setParametr(PHONE.name(), phone);
            try (Statement statmt = conn.createStatement()) {
                statmt.execute(sql);
                // TODO: 08.06.2018 тут же можно уведомить всех клиентов то что этот номер из их списка зарегестрировался
                // НО ЭТИМ КАК-ТО должен заниматься ServerHandler
                return phoneExists(phone);  //можно конечно сказать Ок, но лучше проверимка..
            } catch (Exception e) {
                return false;
            }
        } else {
            return true;
        }
    }
    private boolean phoneExists(String phone){// ------ Проверяет существование логина
        String sql = SQL_USERS_SELECT.setParametr(PHONE.name(),phone);
        try(
                Statement statmt =conn.createStatement() ;
                ResultSet resSet =statmt.executeQuery(sql)
        ) {
            return resSet.getString(PHONE.name()).equals(phone);
        } catch (Exception e){
            return false;
        }
    }
    public long countRegistered(){// ------ Кол-во зареганных
        String sql = "SELECT COUNT(USER_ID) as RES FROM USERS ";
        try (   Statement statmt =conn.createStatement() ;
                ResultSet resSet =statmt.executeQuery(sql)
        ){
            return resSet.getLong(1);
        } catch (Exception e){
            return -1;
        }
    }
    public void closeDB() {// --------Закрытие--------
        try{conn.close();}  catch (Exception ignored){}
        Print(Const.ANSI_COLOR.ANSI_RED,"Соединения закрыты");
    }

    private void Print(Const.ANSI_COLOR color, String str){
        PrintColor(color,"SqlServer : " + str);
    }
    private boolean notTableExists(String table){ // --------Проверяет существование таблицы----
        try(Statement statmt =conn.createStatement() ;
            ResultSet resSet= statmt.executeQuery(
                        "SELECT COUNT(*) " +
                                "FROM sqlite_master " +
                                "WHERE type='table' " +
                                "AND upper(name)=upper('"+table+"')"
            )
        ) {
            return resSet.getInt(1) <= 0;
        } catch (Exception e) {
            return true;
        }
    }
    private static void readResultSet(ResultSet resSet) throws SQLException {
        System.out.println("ResultSet:");
        while(resSet.next()){
            for(int i=1;i <= resSet.getMetaData().getColumnCount();i++){
                String col = resSet.getMetaData().getColumnName(i);
                String res = resSet.getString(i);
                System.out.print("\t\t" + col + " = " + res);
            }
            System.out.println();
        }
        System.out.println("Результат запроса выведен.");
    }
}