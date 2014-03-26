package controllers;import java.util.Calendar;import java.util.Date;import java.util.HashMap;import java.util.List;import java.util.Map;import models.Leads;import org.apache.shiro.SecurityUtils;import org.apache.shiro.authc.AuthenticationException;import org.apache.shiro.authc.IncorrectCredentialsException;import org.apache.shiro.authc.LockedAccountException;import org.apache.shiro.authc.UnknownAccountException;import org.apache.shiro.authc.UsernamePasswordToken;import org.apache.shiro.authz.annotation.Logical;import org.apache.shiro.authz.annotation.RequiresRoles;import org.apache.shiro.subject.Subject;import com.jfinal.core.Controller;import com.jfinal.log.Logger;import com.jfinal.plugin.activerecord.Db;import com.jfinal.plugin.activerecord.Record;public class AppController extends Controller {	private Logger logger = Logger.getLogger(AppController.class);	Subject currentUser = SecurityUtils.getSubject();	private boolean isAuthenticated() {		if (!currentUser.isAuthenticated()) {			redirect("/login");			return false;		}		setAttr("userId", currentUser.getPrincipal());		return true;	}	public void index() {		if (isAuthenticated()) {			setAttr("userId", currentUser.getPrincipal());			render("eeda/index.html");		}	}	public void login() {		if (getPara("username") == null) {			render("eeda/login.html");			return;		}		UsernamePasswordToken token = new UsernamePasswordToken(getPara("username"), getPara("password"));		// ”Remember Me” built-in, just do this:		token.setRememberMe(true);		String errMsg = "";		try {			currentUser.login(token);		} catch (UnknownAccountException uae) {			errMsg = "用户名不存在";			errMsg = "用户名/密码不正确";			uae.printStackTrace();		} catch (IncorrectCredentialsException ice) {			errMsg = "密码不正确";			errMsg = "用户名/密码不正确";			ice.printStackTrace();		} catch (LockedAccountException lae) {			errMsg = "用户名已被冻结";			lae.printStackTrace();		} catch (AuthenticationException ae) {			errMsg = "用户名/密码不正确";			ae.printStackTrace();		}		if (isAuthenticated()) {			setAttr("userId", currentUser.getPrincipal());			redirect("/");		} else {			setAttr("errMsg", errMsg);			redirect("/login");		}	}	public void logout() {		currentUser.logout();		redirect("/login");	}	// 可改成异步，不用刷新页面	@RequiresRoles(value = { "admin", "property_mananger" }, logical = Logical.OR)	public void deleteLeads() {		if (!isAuthenticated())			return;		String id = getPara();		if (id != null) {			Leads l = Leads.dao.findById(id);			l.delete();		}		redirect("/list");	}	public void editLeads() {		if (!isAuthenticated())			return;		String id = getPara();		if (id != null) {			Leads l = Leads.dao.findById(id);			setAttr("leads", l);		} else {			setAttr("leads", new Leads());		}		String queryString = getRequest().getQueryString() == null ? "" : getRequest().getQueryString();		setAttr("queryString", queryString);		render("/eeda/leadsForm.html");	}	public void viewLeads() {		if (!isAuthenticated())			return;		String id = getPara();		if (id != null) {			Leads l = Leads.dao.findById(id);			setAttr("leads", l);		}		render("/eeda/viewLeads.html");	}	public void saveLeads() {		if (!isAuthenticated())			return;		String id = getPara("leadsId");		if (id != "") {			Leads l = Leads.dao.findById(id);		}		// List<UserLogin> users =		// UserLogin.dao.find("select * from user_Login");		// System.out.println("users.size = " + users.size());		// renderText("此方法是一个action");		String title = getPara("title");		// String createDateStr = getPara("createDate");		//		//		// DateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.CHINESE);		//		// try {		// createDate = df.parse(createDateStr);		// } catch (ParseException e) {		// // TODO Auto-generated catch block		// e.printStackTrace();		// }		String remark = getPara("remark");		Record leads = new Record();		leads.set("title", title);		leads.set("priority", getPara("priority"));		leads.set("type", getPara("type"));		leads.set("region", getPara("region"));		leads.set("addr", getPara("addr"));		leads.set("intro", getPara("intro"));		leads.set("remark", getPara("remark"));		leads.set("lowest_price", getPara("lowest_price"));		leads.set("agent_fee", getPara("agent_fee"));		leads.set("introducer", getPara("introducer"));		leads.set("customer_source", getPara("customer_source"));		leads.set("sales", getPara("sales"));		leads.set("follower", getPara("follower"));		leads.set("follower_phone", getPara("follower_phone"));		leads.set("owner", getPara("owner"));		leads.set("owner_phone", getPara("owner_phone"));		leads.set("area", getPara("area"));		leads.set("total", getPara("total"));		leads.set("status", getPara("status"));		leads.set("building_name", getPara("building_name"));		leads.set("building_unit", getPara("building_unit"));		leads.set("building_no", getPara("building_no"));		leads.set("room_no", getPara("room_no"));		leads.set("is_have_car", getPara("is_have_car"));		leads.set("is_public", getPara("is_public"));		if (id != "") {			System.out.println("update....");			leads.set("id", id);			Db.update("leads", leads);		} else {			System.out.println("insert....");			Date createDate = Calendar.getInstance().getTime();			leads.set("create_Date", createDate);			leads.set("creator", currentUser.getPrincipal());			Db.save("leads", leads);		}		redirect("/list");	}	public void list() {		if (!isAuthenticated())			return;		// List<Record> leadsList =		// Db.find("select * from leads order by create_date desc");		// System.out.println("size:" + leadsList.size());		setAttr("userId", currentUser.getPrincipal());		// setAttr("leadsList", leadsList);		render("/eeda/list.html");	}	public void listLeads() throws Exception {		if (!isAuthenticated())			return;		/*		 * Paging		 */		String sLimit = "";		String pageIndex = getPara("sEcho");		if (getPara("iDisplayStart") != null && getPara("iDisplayLength") != null) {			sLimit = " LIMIT " + getPara("iDisplayStart") + ", " + getPara("iDisplayLength");		}		/*		 * Filtering NOTE this does not match the built-in DataTables filtering		 * which does it word by word on any field. It's possible to do here,		 * but concerned about efficiency on very large tables, and MySQL's		 * regex functionality is very limited		 */		String aColumns[] = { "building_name", "status", "type", "region", "area", "total", "intro", "remark", "create_date", "remark" };		String commonFilterWhere = "";		if (getPara("sSearch") != null && !"".equals(getPara("sSearch"))) {			commonFilterWhere = "WHERE (";			for (int i = 0; i < aColumns.length; i++) {				commonFilterWhere += aColumns[i] + " LIKE '%" + getPara("sSearch") + "%' OR ";			}			commonFilterWhere = commonFilterWhere.substring(0, commonFilterWhere.length() - 3);			commonFilterWhere += ')';		}		System.out.println("firtst filter:" + commonFilterWhere);		String colsLength = getPara("iColumns");		String fieldsWhere = "WHERE (";		for (int i = 0; i < Integer.parseInt(colsLength); i++) {			String mDataProp = getPara("mDataProp_" + i);			String searchValue = getPara("sSearch_" + i);			if (searchValue != null && !"".equals(searchValue)) {				if ("AREA".equals(mDataProp) || "TOTAL".equals(mDataProp)) {					String[] range = searchValue.split("-");					fieldsWhere += mDataProp + ">=" + range[0] + " AND ";					if (range.length == 2)						fieldsWhere += mDataProp + "<=" + range[1] + " AND ";				} else {					fieldsWhere += mDataProp + " like '%" + searchValue + "%' AND ";				}			}		}		System.out.println("2nd filter:" + fieldsWhere);		if (fieldsWhere.length() > 8) {			fieldsWhere = fieldsWhere.substring(0, fieldsWhere.length() - 4);			fieldsWhere += ')';		} else {			fieldsWhere = "";		}		String totalWhere = "";		if (commonFilterWhere.length() > 0 && fieldsWhere.length() > 0) {			totalWhere = commonFilterWhere + fieldsWhere.replaceFirst("WHERE", "AND");		}		if (commonFilterWhere.length() > 0 && fieldsWhere.length() == 0) {			totalWhere = commonFilterWhere;		}		if (commonFilterWhere.length() == 0 && fieldsWhere.length() > 0) {			totalWhere = fieldsWhere;		}		String sql = "select count(1) total from leads ";		Record rec = Db.findFirst(sql + totalWhere);		System.out.println(rec.getLong("total"));		sql = "select * from leads " + totalWhere + sLimit;		List<Record> orders = Db.find(sql);// Leads.dao.find(sql);		Map orderMap = new HashMap();		orderMap.put("sEcho", pageIndex);		orderMap.put("iTotalRecords", rec.getLong("total"));		orderMap.put("iTotalDisplayRecords", rec.getLong("total"));		orderMap.put("aaData", orders);		renderJson(orderMap);	}	public void property() {		if (!isAuthenticated())			return;		render("/eeda/property.html");	}	public void registrationFlow() {		if (!isAuthenticated())			return;		render("/eeda/registrationFlow.html");	}	public void pos() {		if (!isAuthenticated())			return;		render("/eeda/pos.html");	}	public void visa() {		if (!isAuthenticated())			return;		render("/eeda/visa.html");	}	public void loanIntro() {		if (!isAuthenticated())			return;		render("/eeda/loanIntro.html");	}}