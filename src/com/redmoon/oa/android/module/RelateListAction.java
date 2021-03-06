package com.redmoon.oa.android.module;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.js.fan.db.ListResult;
import cn.js.fan.db.ResultIterator;
import cn.js.fan.db.ResultRecord;
import cn.js.fan.util.ErrMsgException;
import cn.js.fan.util.ParamUtil;
import cn.js.fan.util.StrUtil;

import com.cloudwebsoft.framework.db.JdbcTemplate;
import com.redmoon.oa.LogUtil;
import com.redmoon.oa.android.Privilege;
import com.redmoon.oa.android.base.BaseAction;
import com.redmoon.oa.flow.FormDb;
import com.redmoon.oa.flow.FormField;
import com.redmoon.oa.flow.macroctl.MacroCtlMgr;
import com.redmoon.oa.flow.macroctl.MacroCtlUnit;
import com.redmoon.oa.person.UserDb;
import com.redmoon.oa.person.UserMgr;
import com.redmoon.oa.visual.FormDAO;
import com.redmoon.oa.visual.FuncUtil;
import com.redmoon.oa.visual.ModulePrivDb;
import com.redmoon.oa.visual.ModuleRelateDb;
import com.redmoon.oa.visual.ModuleSetupDb;
import com.redmoon.oa.visual.ModuleUtil;
import com.redmoon.oa.visual.SQLBuilder;

/**
 * @Description: 
 * @author: 
 * @Date: 2017-8-13下午08:29:05
 */
public class RelateListAction extends BaseAction {
	private String moduleCode = "";
	private String skey = "";
	private int pageSize = 15;
	private int pageNum = 1;
	private String formCodeRelated = "";
		
	public String getFormCodeRelated() {
		return formCodeRelated;
	}

	public void setFormCodeRelated(String formCodeRelated) {
		this.formCodeRelated = formCodeRelated;
	}

	private long parentId = -1;
	
	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	/**
	 * @return the pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * @param pagesize the pageSize to set
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	private String op = "";
	
	private String orderBy = "id";
	private String sort = "desc";
	
	/**
	 * @return the orderBy
	 */
	public String getOrderBy() {
		return orderBy;
	}

	/**
	 * @param orderBy the orderBy to set
	 */
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	/**
	 * @return the sort
	 */
	public String getSort() {
		return sort;
	}

	/**
	 * @param sort the sort to set
	 */
	public void setSort(String sort) {
		this.sort = sort;
	}

	/**
	 * @return the moduleCode
	 */
	public String getModuleCode() {
		return moduleCode;
	}

	/**
	 * @param moduleCode the moduleCode to set
	 */
	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	/**
	 * @return the op
	 */
	public String getOp() {
		return op;
	}

	/**
	 * @param op the op to set
	 */
	public void setOp(String op) {
		this.op = op;
	}
	
	public String getSkey() {
		return skey;
	}

	public void setSkey(String skey) {
		this.skey = skey;
	}


	/**
	 * @return the pageNum
	 */
	public int getPageNum() {
		return pageNum;
	}

	/**
	 * @param pagenum the pageNum to set
	 */
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	@Override
	public void executeAction() {
		super.executeAction();
		
		if ("".equals(moduleCode)) {
			return;
		}	
		
	    try {
			ModuleSetupDb msd = new ModuleSetupDb();
			msd = msd.getModuleSetupDb(moduleCode);
			if (msd==null) {
				jResult.put(RETURNCODE,RESULT_MODULE_ERROR);//表单不存在
				return;
			}

			String formCode = msd.getString("form_code");
			
			UserMgr um = new UserMgr();
	    	Privilege privilege = new Privilege();
			boolean re = privilege.Auth(getSkey());
			jReturn.put(RES,RETURNCODE_SUCCESS); //请求成功
			if(re){
				jResult.put(RETURNCODE,RESULT_TIME_OUT); //登录超时
			}else{
				HttpServletRequest request = ServletActionContext.getRequest();				
				String userName = privilege.getUserName(skey);
				if (userName==null || "".equals(userName)) {
					com.redmoon.oa.pvg.Privilege pvg = new com.redmoon.oa.pvg.Privilege();
					userName = pvg.getUser(request);
				}						
			
				if(formCode == null || formCode.trim().equals("")){
					jResult.put(RETURNCODE,RESULT_FORMCODE_ERROR); //表单为空
				}else{
					FormDb fdMain = new FormDb();
					fdMain = fdMain.getFormDb(formCode); 
					if (!fdMain.isLoaded()) { //表单不存在
						jResult.put(RETURNCODE,RESULT_FORMCODE_ERROR);//表单不存在
						return;
					}else{
						FormDb fdRelated = new FormDb();
						
						try {
							request.setCharacterEncoding("utf-8");
						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						// 按选项卡设置中的顺序排列，从0开始
						int subTagIndex = ParamUtil.getInt(request, "subTagIndex", -1);
						
						if (subTagIndex!=-1) {
							String[] subTags = StrUtil.split(StrUtil.getNullStr(msd.getString("sub_nav_tag_name")), "\\|");
							int subLen = 0;
							if (subTags!=null)
								subLen = subTags.length;
							String tagName = "";
							for (int i=0; i<subLen; i++) {
								if (i==subTagIndex) {
									tagName = subTags[i];
									break;
								}
							}
							
							String tagUrl = ModuleUtil.getModuleSubTagUrl(moduleCode, tagName);
					    	if (tagUrl.equals("")) {
					    		com.cloudwebsoft.framework.util.LogUtil.getLog(getClass()).error(tagName + " 不存在！");
					    	}
					    	else {
						    	if (tagUrl.startsWith("{")) {
									JSONObject json = new JSONObject(tagUrl);
									if (!json.isNull("formRelated")) {
										// formCodeRelated = json.getString("formRelated");
										msd = msd.getModuleSetupDb(json.getString("formRelated"));
										// 用于SQLBuilder中调用ModuleUtil.parseFilter时
										request.setAttribute("MODULE_SETUP", msd);
										formCodeRelated = msd.getString("form_code");		
									}									
						    	}
					    	}
						}
						
						fdRelated = fdRelated.getFormDb(formCodeRelated);
						
						MacroCtlUnit mu;
						MacroCtlMgr mm = new MacroCtlMgr();
						
						FormDAO fdao = new FormDAO();

						com.redmoon.oa.visual.FormDAOMgr fdm = new com.redmoon.oa.visual.FormDAOMgr(formCode);
						String relateFieldValue = fdm.getRelateFieldValue(parentId, formCodeRelated);
						if (relateFieldValue == null) {
							relateFieldValue = SQLBuilder.IS_NOT_RELATED;
							
/*							jReturn.put(RES,-3);
							jReturn.put("msg", "请检查模块是否相关联！");
							setResult(jReturn.toString());												
							return;			*/				
						}			
						
						jReturn.put(RES, 0);
						jReturn.put("parentId", parentId);
						jReturn.put("moduleCode", moduleCode);
						jReturn.put("formCodeRelated", formCodeRelated);
						
						// 关联模块
						JSONArray arrRelated = new JSONArray();
						ModuleRelateDb mrdTop = new ModuleRelateDb();
						java.util.Iterator irTop = mrdTop.getModulesRelated(formCode).iterator();
						while (irTop.hasNext()) {
							mrdTop = (ModuleRelateDb)irTop.next();
							// 有查看权限才能看到从模块选项卡
							ModulePrivDb mpdTop = new ModulePrivDb(mrdTop.getString("relate_code"));
							if (mpdTop.canUserSee(userName)) {	
								String name = fdRelated.getFormDb(mrdTop.getString("relate_code")).getName();
								JSONObject jsonRelated = new JSONObject();
								jsonRelated.put("name", name);
								jsonRelated.put("formCodeRelated", mrdTop.getString("relate_code"));					
								arrRelated.put(jsonRelated);
							}
						}
						jReturn.put("formRelated", arrRelated);					

						ModuleSetupDb msdRelated = new ModuleSetupDb();
						msdRelated = msdRelated.getModuleSetupDb(formCodeRelated);
						String btnName = StrUtil.getNullStr(msdRelated.getString("btn_name"));
						String[] btnNames = StrUtil.split(btnName, ",");
						String btnScript = StrUtil.getNullStr(msdRelated.getString("btn_script"));
						String[] btnScripts = StrUtil.split(btnScript, "#");						
						
						// 取得条件
						JSONArray conditions = new JSONArray();
						int len = 0;
						if (btnNames!=null) {
							len = btnNames.length;
							for (int i=0; i<len; i++) {
								if (btnScripts[i].startsWith("{")) {
									// System.out.println(getClass() + " " + btnScripts[i]);
									JSONObject jsonBtn = new JSONObject(btnScripts[i]);
									if (((String)jsonBtn.get("btnType")).equals("queryFields")) {
										String condFields = (String)jsonBtn.get("fields");
										String[] fieldAry = StrUtil.split(condFields, ",");
										for (int j=0; j<fieldAry.length; j++) {
											String fieldName = fieldAry[j];
											FormField ff = fdRelated.getFormField(fieldName);
											if (ff==null) {
												continue;
											}
											String condType = (String)jsonBtn.get(fieldName);
											String queryValue = ParamUtil.get(request, fieldName);
											
											JSONObject jo = new JSONObject();
											
											jo.put("fieldName", ff.getName());
											jo.put("fieldTitle", ff.getTitle());
											jo.put("fieldType", ff.getFieldType());
											jo.put("fieldCond", condType); // 点时间、时间段或模糊、准确查询
											jo.put("fieldOptions", "");
											jo.put("fieldValue", queryValue);
											
						               		if (ff.getType().equals(FormField.TYPE_DATE) || ff.getType().equals(FormField.TYPE_DATE_TIME)) {
												if (condType.equals("0")) {
													String fDate = ParamUtil.get(request, ff.getName() + "FromDate");
													String tDate  = ParamUtil.get(request, ff.getName() + "ToDate");
													jo.put("fromDate", fDate);
													jo.put("toDate", tDate);
												}
											}
											conditions.put(jo);
										}
									}
								}
							}
						}
						jResult.put("conditions", conditions);
						
						request.setAttribute("cwsId", String.valueOf(parentId));
						// String[] ary = SQLBuilder.getModuleListSqlAndUrlStr(request, fd, op, orderBy, sort);
						String[] ary = SQLBuilder.getModuleListRelateSqlAndUrlStr(request, fdRelated, op, orderBy, sort, relateFieldValue);
						String sql = ary[0];
						
						String listField = StrUtil.getNullStr(msdRelated.getString("list_field"));
						String[] formFieldArr = StrUtil.split(listField, ",");
						if(formFieldArr != null && formFieldArr.length>0){
							ListResult lr = fdao.listResult(formCodeRelated, sql, pageNum, pageSize);
							int total = lr.getTotal();
							if(total == 0){
								jResult.put(RETURNCODE,RESULT_NO_DATA); //没有数据
							}else{
								jResult.put("total", total); //总页数
								jResult.put(RETURNCODE,RESULT_SUCCESS); //请求成功
								Iterator ir = null;
								Vector v = lr.getResult();
								JSONArray dataArr = new JSONArray();
								if (v != null){
									ir = v.iterator();
								}
								
								// 增加权限控制
								ModulePrivDb mpd = new ModulePrivDb(formCodeRelated);
								boolean canAdd = mpd.canUserAppend(userName);
								boolean canEdit = mpd.canUserModify(userName);
								boolean canDel = mpd.canUserManage(userName);
								jResult.put("canAdd", canAdd);
								jResult.put("canEdit", canEdit);
								jResult.put("canDel", canDel);								
								
								// 列名
								JSONArray cols = new JSONArray();
								jResult.put("cols", cols);
								
								int i = 0;
								HashMap<String,FormField> map = getFormFieldsByFromCode(formCode);
								while (ir != null && ir.hasNext()) {
									fdao = (FormDAO) ir.next();
									long id  = fdao.getId();
									JSONObject rowObj = new JSONObject();
									rowObj.put("id", id);
									rowObj.put("creator", um.getUserDb(fdao.getCreator()).getRealName());
									
									JSONArray fieldArr = new JSONArray();
									rowObj.put("fields", fieldArr);
									for(String fieldName : formFieldArr) {
										JSONObject formFieldObj = new JSONObject(); //Form表单对象
										// System.out.println(ListAction.class.getName() + " fieldName=" + fieldName);
										String title = "";
										if (fieldName.startsWith("main:")) {
											String[] subFields = StrUtil.split(fieldName, ":");
											if (subFields.length == 3) {
												FormDb subfd = new FormDb(subFields[1]);
												title = subfd.getFieldTitle(subFields[2]);
											}
										} else if (fieldName.startsWith("other:")) {
											String[] otherFields = StrUtil.split(fieldName, ":");
											if (otherFields.length == 5) {
												FormDb otherFormDb = new FormDb(otherFields[2]);
												title = otherFormDb.getFieldTitle(otherFields[4]);
											}
										} else if (fieldName.equals("cws_creator")) {
											title = "创建者";
										}
										else if (fieldName.equals("ID")) {
											title = "ID";
										}
										else if (fieldName.equals("cws_progress")) {
											title = "进度";
										}
										else {
											title = fdRelated.getFieldTitle(fieldName);
										}	
										
										formFieldObj.put("name", fieldName);										
										formFieldObj.put("title", title);
										
										if (i==0) {
											JSONObject jo = new JSONObject();
											jo.put("name", fieldName);
											jo.put("title", title);
											cols.put(jo);
										}

										if(map.containsKey(fieldName)) {
											FormField ff = map.get(fieldName);
											String value = fdao.getFieldValue(fieldName); //表单值
											formFieldObj.put("value", value);
											String type = ff.getType();// 类型描述
											formFieldObj.put("type", type);
										}
										
										String controlText = "";
										if (fieldName.startsWith("main:")) {
											String[] subFields = fieldName.split(":");
											if (subFields.length == 3) {
												FormDb subfd = new FormDb(subFields[1]);
												com.redmoon.oa.visual.FormDAO subfdao = new com.redmoon.oa.visual.FormDAO(subfd);
												FormField subff = subfd.getFormField(subFields[2]);
												String subsql = "select id from " + subfdao.getTableName() + " where cws_id=" + id + " order by cws_order";
												JdbcTemplate jt = new JdbcTemplate();
												StringBuilder sb = new StringBuilder();
												try {
													ResultIterator ri = jt.executeQuery(subsql);
													while (ri.hasNext()) {
														ResultRecord rr = (ResultRecord) ri.next();
														int subid = rr.getInt(1);
														subfdao = new com.redmoon.oa.visual.FormDAO(subid, subfd);
														String subFieldValue = subfdao.getFieldValue(subFields[2]);
														if (subff != null && subff.getType().equals(FormField.TYPE_MACRO)) {
															mu = mm.getMacroCtlUnit(subff.getMacroType());
															if (mu != null) {
																subFieldValue = mu.getIFormMacroCtl().converToHtml(request, subff, subFieldValue);
															}
														}
														sb.append("<span>").append(subFieldValue).append("</span>").append(ri.hasNext() ? "</br>" : "");
													}
												} catch (Exception e) {
													e.printStackTrace();
												}
												controlText = sb.toString();
											}
										} else if (fieldName.startsWith("other:")) {
											controlText = com.redmoon.oa.visual.FormDAOMgr.getFieldValueOfOther(request, fdao, fieldName);
										} 
										else if (fieldName.equals("ID")) {
											controlText = String.valueOf(fdao.getId());
										}
										else if (fieldName.equals("cws_progress")) {
											controlText = String.valueOf(fdao.getCwsProgress());
										}
										else if (fieldName.equals("cws_creator")) {
											String realName = "";
											if (fdao.getCreator()!=null) {
											UserDb user = um.getUserDb(fdao.getCreator());
											if (user!=null)
												realName = user.getRealName();
											}
											controlText = realName;
										}
										else {
											FormField ff = fdao.getFormField(fieldName);
											if (ff==null) {
												controlText = fieldName + " 已不存在！";
											}
											else {
												if (ff.getType().equals(FormField.TYPE_MACRO)) {
													mu = mm.getMacroCtlUnit(ff.getMacroType());
													if (mu != null) {
														controlText = mu.getIFormMacroCtl().converToHtml(request, ff, fdao.getFieldValue(fieldName));
													}
												}
												else {
													controlText = FuncUtil.renderFieldValue(fdao, ff);
												}
											}
										}		
										
										formFieldObj.put("text", controlText);
										
										fieldArr.put(formFieldObj);								
									}
									dataArr.put(rowObj);//组装json数组
									
									i++;
								}
								
								jResult.put(DATAS, dataArr);
							}
						}
					}
				}
			}
			jReturn.put(RESULT, jResult);
		} catch (JSONException e) {
			Logger.getLogger(RelateListAction.class.getName()).error(e.getMessage());
		} catch (ErrMsgException e) {
			Logger.getLogger(RelateListAction.class.getName()).error(e.getMessage());
		}
	}
	
	/**
	 * 获得所有字段信息
	 * @return
	 */
	public HashMap<String,FormField> getFormFieldsByFromCode(String formCode){
		HashMap<String,FormField> map = new HashMap<String, FormField>();
		FormDb fd = new FormDb();
		fd = fd.getFormDb(formCode); 
		FormDAO fdao = new FormDAO(fd);//获得所有表单元素
		Iterator fdaoIr = fdao.getFields().iterator();
		while(fdaoIr!=null && fdaoIr.hasNext()){
			FormField ff = (FormField)fdaoIr.next();
			map.put(ff.getName(), ff);
		}
		return map;
		
	}

}
