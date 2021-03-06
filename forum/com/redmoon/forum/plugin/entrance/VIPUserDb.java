package com.redmoon.forum.plugin.entrance;

import java.sql.*;
import java.util.Date;
import java.util.Vector;

import cn.js.fan.base.ObjectDb;
import cn.js.fan.db.*;
import cn.js.fan.util.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class VIPUserDb extends ObjectDb {

    public VIPUserDb() {
        init();
    }

    public VIPUserDb(String userName) {
        this.userName = userName;
        init();
        load();
    }

    public boolean create() {
        int rowcount = 0;
        Conn conn = null;
        try {
            conn = new Conn(connname);
            PreparedStatement ps = conn.prepareStatement(this.QUERY_CREATE);
            ps.setString(1, userName);
            ps.setString(2, DateUtil.toLongString(beginDate));
            ps.setString(3, DateUtil.toLongString(endDate));
            ps.setString(4, boards);
            ps.setInt(5, valid?1:0);
            rowcount = conn.executePreUpdate();
        } catch (SQLException e) {
            logger.error("create:" + e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return rowcount>0? true:false;
    }

    /**
     * del
     *
     * @return boolean
     * @throws ErrMsgException
     * @throws ResKeyException
     * @todo Implement this cn.js.fan.base.ObjectDb method
     */
    public boolean del() throws ErrMsgException, ResKeyException {
        int rowcount = 0;
        Conn conn = null;
        try {
            conn = new Conn(connname);
            PreparedStatement ps = conn.prepareStatement(this.QUERY_DEL);
            ps.setString(1, userName);
            rowcount = conn.executePreUpdate();
            VIPUserCache uc = new VIPUserCache(this);
            primaryKey.setValue(userName);
            uc.refreshDel(primaryKey);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return rowcount>0? true:false;
    }

    /**
     *
     * @param pk Object
     * @return Object
     * @todo Implement this cn.js.fan.base.ObjectDb method
     */
    public ObjectDb getObjectRaw(PrimaryKey pk) {
        return new VIPUserDb(pk.getStrValue());
    }

    public VIPUserDb getVIPUserDb(String id) {
        return (VIPUserDb)getObjectDb(id);
    }

    /**
     * load
     *
     * @throws ErrMsgException
     * @throws ResKeyException
     * @todo Implement this cn.js.fan.base.ObjectDb method
     */
    public void load() {
        // "SELECT beginDate,endDate,userName,boards,isValid FROM " + tableName + " WHERE id=?";
        ResultSet rs = null;
        Conn conn = new Conn(connname);
        try {
            PreparedStatement ps = conn.prepareStatement(this.QUERY_LOAD);
            ps.setString(1, userName);
            primaryKey.setValue(userName);
            rs = conn.executePreQuery();
            if (rs.next()) {
                beginDate = DateUtil.parse(rs.getString(1));
                endDate = DateUtil.parse(rs.getString(2));
                boards = StrUtil.getNullStr(rs.getString(3));
                valid = rs.getInt(4)==1?true:false;
                loaded = true;
            }
        } catch (SQLException e) {
            logger.error("load:" + e.getMessage());
        }
        finally {
            if (conn!=null) {
                conn.close();
                conn = null;
            }
        }
    }

    /**
     * save
     *
     * @return boolean
     * @throws ErrMsgException
     * @throws ResKeyException
     * @todo Implement this cn.js.fan.base.ObjectDb method
     */
    public boolean save() throws ErrMsgException, ResKeyException {
        // "UPDATE " + tableName + " SET beginDate=?, endDate=?, userName=?,boards=?,isValid=? WHERE id=?";

        int rowcount = 0;
        Conn conn = null;
        try {
            conn = new Conn(connname);
            PreparedStatement ps = conn.prepareStatement(this.QUERY_SAVE);
            ps.setString(1, DateUtil.toLongString(beginDate));
            ps.setString(2, DateUtil.toLongString(endDate));
            ps.setString(3, boards);
            ps.setInt(4, valid?1:0);
            ps.setString(5, userName);
            rowcount = conn.executePreUpdate();
            VIPUserCache uc = new VIPUserCache(this);
            primaryKey.setValue(userName);
            uc.refreshSave(primaryKey);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return rowcount>0? true:false;
    }

    public ListResult listResult(String listsql, int curPage, int pageSize) throws
            ErrMsgException {
        int total = 0;
        ResultSet rs = null;
        ListResult lr = new ListResult();
        Vector result = new Vector();
        lr.setTotal(total);
        lr.setResult(result);
        Conn conn = new Conn(connname);
        try {
            // 取得总记录条数
            String countsql = SQLFilter.getCountSql(listsql);
            rs = conn.executeQuery(countsql);
            if (rs != null && rs.next()) {
                total = rs.getInt(1);
            }
            if (rs != null) {
                rs.close();
                rs = null;
            }

            if (total != 0)
                conn.setMaxRows(curPage * pageSize); //尽量减少内存的使用

            rs = conn.executeQuery(listsql);
            if (rs == null) {
                return lr;
            } else {
                rs.setFetchSize(pageSize);
                int absoluteLocation = pageSize * (curPage - 1) + 1;
                if (rs.absolute(absoluteLocation) == false) {
                    return lr;
                }
                do {
                    VIPUserDb cmm = getVIPUserDb(rs.getString(1));
                    result.addElement(cmm);
                } while (rs.next());
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new ErrMsgException("listResult:DataBase error！");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {}
                rs = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        lr.setResult(result);
        lr.setTotal(total);
        return lr;
    }

    public void initDB() {
        tableName = "plugin_entrance_vip_user";
        primaryKey = new PrimaryKey("userName", PrimaryKey.TYPE_STRING);
        objectCache = new VIPUserCache(this);

        this.QUERY_DEL =
                "delete FROM " + tableName + " WHERE userName=?";
        this.QUERY_CREATE =
                "INSERT into " + tableName + " (userName, beginDate, endDate, boards, isValid) VALUES (?,?,?,?,?)";
        this.QUERY_LOAD =
                "SELECT beginDate,endDate,boards,isValid FROM " + tableName + " WHERE userName=?";
        this.QUERY_SAVE =
                "UPDATE " + tableName + " SET beginDate=?, endDate=?, boards=?,isValid=? WHERE userName=?";
        isInitFromConfigDB = false;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUserName() {
        return userName;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isValid() {
        return valid;
    }

    public String getBoards() {
        return boards;
    }

    public void setBoards(String boards) {
        this.boards = boards;
    }

    private String userName;
    private Date beginDate;
    private Date endDate;

    private boolean valid;
    private String boards;
}
