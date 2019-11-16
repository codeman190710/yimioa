<%@ page contentType="text/html;charset=gb2312"%><%@page import="cn.js.fan.util.*"%><%@page import="cn.js.fan.web.*"%><%@page import="com.redmoon.forum.*"%><%@page import="com.redmoon.forum.person.*"%><%@page import="com.redmoon.forum.plugin.*"%><%@page import="com.redmoon.forum.plugin.base.*"%><%@page import="java.io.*"%><jsp:useBean id="privilege" scope="page" class="com.redmoon.forum.Privilege"/><%
if (!Privilege.isUserLogin(request)) {
	if (!ForumDb.getInstance().canGuestSeeAttachment()) {
		response.sendRedirect("../info.jsp?op=login&info=" + StrUtil.UrlEncode(SkinUtil.LoadString(request, "info_please_login")));
		return;	
	}
}

int msgId = ParamUtil.getInt(request, "msgId");

MsgDb md = new MsgDb();
md = md.getMsgDb(msgId);

if (md==null || !md.isLoaded()) {
	out.print("<meta http-equiv='Content-Type' content='text/html; charset=gb2312'>");
	out.println("Topic not found!");
	return;
}

// ���������Ȧ����е�����
if (!com.redmoon.forum.plugin.group.GroupPrivilege.canUserSee(request, md)) {
	response.sendRedirect("../info.jsp?info=" + StrUtil.UrlEncode("ֻ��Ȧ�ڳ�Ա���ܷ���"));
	return;
}

if (!privilege.canUserDo(request, md.getboardcode(), "attach_download")) {
	response.sendRedirect("../info.jsp?info=" + StrUtil.UrlEncode(SkinUtil.LoadString(request, "pvg_invalid")));
	return;
}

int attachId = ParamUtil.getInt(request, "attachId");
Attachment att = md.getAttachment(attachId);

// ���ø��������Ƿ��踶��
String groupCode = "";
if (Privilege.isUserLogin(request)) {
   UserDb ud = new UserDb();
   ud = ud.getUser(Privilege.getUser(request));
   groupCode = ud.getGroupCode();
   // ȡ���û�������
   if (groupCode.equals(""))
   		groupCode = UserGroupDb.EVERYONE;
}
else {
	groupCode = UserGroupDb.GUEST;
}

// ����û��ڴ˰�������Ƿ��踶��
UserGroupPrivDb ugpd = new UserGroupPrivDb();
ugpd = ugpd.getUserGroupPrivDb(groupCode, md.getboardcode());
String moneyCode = StrUtil.getNullStr(ugpd.getString("money_code"));
if (!moneyCode.equals("")) {
	int sum = ugpd.getInt("money_sum");
	if (sum>0) {
		// ����û��Ľ���Ƿ��㹻
		ScoreMgr sm = new ScoreMgr();
		ScoreUnit su = sm.getScoreUnit(moneyCode);
		IPluginScore isc = su.getScore();
		if (isc != null) {
		   try {
			   isc.pay(Privilege.getUser(request), IPluginScore.SELLER_SYSTEM, sum);
		   } catch (ResKeyException e) {
				out.print(SkinUtil.makeErrMsg(request, e.getMessage(request)));
				return;
		   }
		}
	}
}

com.redmoon.forum.Config cfg = com.redmoon.forum.Config.getInstance();

if (att.isRemote()) {
	// String url = cfg.getProperty("forum.ftpUrl");
	// response.sendRedirect(url + "/" + att.getVisualPath() + "/" + att.getDiskName());
	
	FTPUtil ftp = new FTPUtil();
    boolean retFtp = ftp.connect(cfg.getProperty("forum.ftpServer"),
    				  	cfg.getIntProperty("forum.ftpPort"),
                        cfg.getProperty("forum.ftpUser"),
                        cfg.getProperty("forum.ftpPwd"), true);
    if (!retFtp) {
    	ftp.close();
	    out.print(ftp.getReplyMessage());
		return;
    }
		
	String remotefile = att.getVisualPath() + "/" + att.getDiskName(); 
	InputStream is = null; 
	
	is = ftp.getFTPClient().retrieveFileStream(remotefile); 

	response.setContentType("application/octet-stream");
	response.setHeader("Content-disposition","attachment; filename=" + StrUtil.GBToUnicode(att.getName()));

	BufferedInputStream bis = null;
	BufferedOutputStream bos = null;
	try {
		bis = new BufferedInputStream(is);
		bos = new BufferedOutputStream(response.getOutputStream());
		
		byte[] buff = new byte[2048];
		int bytesRead;
		
		while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
			bos.write(buff,0,bytesRead);
		}
	} catch(final IOException e) {
		System.out.println(getClass() + "IOException: " + e);
	} finally {
		if (bis != null)
			bis.close();
		if (bos != null)
			bos.close();
		if (is != null) { 
			is.close(); 
		}
		ftp.completePendingCommand();
		ftp.close();
	}

	att.setDownloadCount(att.getDownloadCount() + 1);
	att.save();	
	return;
}

// response.setContentType(MIMEMap.get(StrUtil.getFileExt(att.getDiskName())));
// response.setContentType(MIMEMap.get(att.getExt()));
// response.setHeader("Content-disposition","filename=" + StrUtil.GBToUnicode(att.getName()));

// ��ѯ�����صķ�ʽ�򿪣��Ḳ�Ǹ�����
response.setContentType("application/octet-stream");
response.setHeader("Content-disposition","attachment; filename=" + StrUtil.GBToUnicode(att.getName()));

BufferedInputStream bis = null;
BufferedOutputStream bos = null;

String filePath = Global.realPath + cfg.getAttachmentPath() + "/" + att.getVisualPath() + "/" + att.getDiskName();
try {
	bis = new BufferedInputStream(new FileInputStream(filePath)); // att.getFullPath()));
	bos = new BufferedOutputStream(response.getOutputStream());
	
	byte[] buff = new byte[2048];
	int bytesRead;
	
	while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
		bos.write(buff,0,bytesRead);
	}
} catch(final IOException e) {
	System.out.println("IOException: " + e);
} finally {
	if (bis != null)
		bis.close();
	if (bos != null)
		bos.close();
}

att.setDownloadCount(att.getDownloadCount() + 1);
att.save();
%>