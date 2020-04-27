package com.makesiPe.buildCode;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @className 代码生成器
 * 
 * @author makesiPe
 * @description 根据pdm自动生成entity、dao、service
 */
public class BuildCodeUtil {

	public static Properties basePro = new Properties();//基本参数配置文件
	public static Properties detailPro = new Properties();//具体配置文件
	public static BufferedReader readBase;
	public static BufferedReader readDetail;
	static {
		try {
			Resource readBase_resource = new ClassPathResource("properties/createCode_base.properties");
			Resource readDetail_resource = new ClassPathResource("properties/createCode.properties");
			readBase = new BufferedReader(new InputStreamReader(readBase_resource.getInputStream()));
			readDetail = new BufferedReader(new InputStreamReader(readDetail_resource.getInputStream()));
			basePro.load(readBase);
			detailPro.load(readDetail);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String sourceFilePath = basePro.getProperty("basePDM")+detailPro.getProperty("pdm");//pdm地址
	private static String tableCodes = detailPro.getProperty("tables");//生成表名
	private static String filePath = basePro.getProperty("filePath");//生成文件目录
	private static String sqlFilePath = basePro.getProperty("sqlFilePath");//sql脚本文件
	
	private static String PackageName = basePro.getProperty("PackageName");//包名
	private static String moduleName = basePro.getProperty("moduleName");//当前模块名

	//类注释
	private static String author = basePro.getProperty("author");
	private static String nowDate = DateUtils.formatDate(new Date(), "yyyy.MM.dd");
	
	//具体类名后缀
	private static String className_dao = basePro.getProperty("className_dao");
	private static String className_dao_impl = basePro.getProperty("className_dao_impl");
	private static String className_service = basePro.getProperty("className_service");
	private static String className_service_impl = basePro.getProperty("className_service_impl");
	
	//具体类包类型
	private static String classPackage_entity = basePro.getProperty("classPackage_entity");
	private static String classPackage_dao = basePro.getProperty("classPackage_dao");
	private static String classPackage_service = basePro.getProperty("classPackage_service");
	
	//基础类
	private static String baseEntity = basePro.getProperty("baseEntity");
	private static String baseDao = basePro.getProperty("baseDao");
	private static String baseDaoImpl = basePro.getProperty("baseDaoImpl");
	private static String baseService = basePro.getProperty("baseService");
	private static String baseServiceImpl = basePro.getProperty("baseServiceImpl");
	
	
	public static void main(String []args) throws Exception {
		buildCode();
		
	}
	
	
	/**
	 * 生成数据库表创建脚本
	 */
	public static void buildSql() {
		if(StringUtils.isBlank(sourceFilePath) || StringUtils.isBlank(tableCodes)) {
			System.out.println("请先配置要生成的相应pdm和表");
			try {
				readBase.close();
				readDetail.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		String[] tables = tableCodes.split(",");
		
		File sqlFile = new File(sqlFilePath);
		FileOutputStream out = null;
		BufferedOutputStream bout = null;
		try {
			out = new FileOutputStream(sqlFile);
			bout = new BufferedOutputStream(out);
			
			for(String tableCode : tables) {
				Resource sourcePdm = new ClassPathResource(sourceFilePath);
				if(sourcePdm != null) {
					InputStream in = null;
					try {
						in = sourcePdm.getInputStream();
						SAXReader reader = new SAXReader();
						Document document = reader.read(in);
						
						Iterator itr = document.selectNodes("//c:Tables//o:Table").iterator();//表区域
						while (itr.hasNext()) {
							Element e_table = (Element) itr.next();
							String code = e_table.elementTextTrim("Code");//表名
							if(tableCode.equals(code)) {
								String tableCNName = e_table.elementTextTrim("Name");//表中文名
								String tableZNName = e_table.elementTextTrim("Code");//表名
								
								//打印内容
								System.out.println(tableCNName +"-"+ tableZNName);
								List list = e_table.element("Columns").elements("Column");
								Iterator columnList = list.iterator();
								while (columnList.hasNext()) {
									Element e_col = (Element) columnList.next();
									String name = e_col.elementTextTrim("Name");
									String columnCode = e_col.elementTextTrim("Code");
									String dataType = e_col.elementTextTrim("DataType");
									
									System.out.println(name+"-"+columnCode+"-"+dataType);
								}
								
								//写sql
								createSQL(bout, tableZNName, tableCNName, list);
							}
						}
					}catch(Exception e) {
						try {
							in.close();
							readBase.close();
							readDetail.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			try {
				bout.flush();
				bout.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 数据库表创建脚本
	 * @param bout			sql文件输出流
	 * @param tableZnName	表名
	 * @param tableCnName	表中文名
	 * @param columnList	字段列
	 */
	public static void createSQL(OutputStream bout, String tableZnName, String tableCnName, List columnList) throws Exception {
		StringBuilder sql = new StringBuilder();
		Iterator columnIt = columnList.iterator();
		sql.append("drop table if exists "+tableZnName+";\r\n" + 
				"\r\n" + 
				"/*==============================================================*/\r\n" + 
				"/* Table: "+tableZnName+"                                      */\r\n" + 
				"/*==============================================================*/\r\n" + 
				"create table "+tableZnName+"\r\n" + 
				"(\r\n");
		int i = 0;
		String primaryKey = "";
		while (columnIt.hasNext()) {
			i++;
			Element e_col = (Element) columnIt.next();
			String name = e_col.elementTextTrim("Name");
			String columnCode = e_col.elementTextTrim("Code");
			String dataType = e_col.elementTextTrim("DataType");
			if(i == 1) {//主键
				primaryKey = columnCode;
				sql.append("   "+columnCode+"                 "+dataType+" not null comment '"+name+"',\r\n");
			}else {
				sql.append("   "+columnCode+"                 "+dataType+" comment '"+name+"', \r\n");
			}
		}
		sql.append("   primary key ("+primaryKey+")\r\n); \r\n \r\n \r\n");
		
		//写入脚本文件
		System.out.println("sql脚本【"+tableZnName+"】开始写入=====================");
		bout.write(sql.toString().getBytes());
		bout.flush();
		System.out.println("sql脚本【"+tableZnName+"】写入完成=====================");
	}
	
	/**
	 * 生成基础类文件
	 */
	public static void buildCode(){
		if(StringUtils.isBlank(sourceFilePath) || StringUtils.isBlank(tableCodes)) {
			System.out.println("请先配置要生成的相应pdm和表");
			try {
				readBase.close();
				readDetail.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		String[] tables = tableCodes.split(",");
		
		File sqlFile = new File(sqlFilePath);
		FileOutputStream out = null;
		BufferedOutputStream bout = null;
		try {
			out = new FileOutputStream(sqlFile);
			bout = new BufferedOutputStream(out);
			
			String successTable = "";
			for(String tableCode : tables) {
				Resource sourcePdm = new ClassPathResource(sourceFilePath);
				if(sourcePdm != null) {
					InputStream in = null;
					try {
						in = sourcePdm.getInputStream();
						SAXReader reader = new SAXReader();
						Document document = reader.read(in);
						
						Iterator itr = document.selectNodes("//c:Tables//o:Table").iterator();//表区域
						while (itr.hasNext()) {
							Element e_table = (Element) itr.next();
							String code = e_table.elementTextTrim("Code");//表名
							if(tableCode.equals(code)) {
								String tableCNName = e_table.elementTextTrim("Name");//表中文名
								String tableZNName = e_table.elementTextTrim("Code");//表名
								
								//打印内容
								System.out.println(tableCNName +"-"+ tableZNName);
								List list = e_table.element("Columns").elements("Column");
								Iterator columnList = list.iterator();
								while (columnList.hasNext()) {
									Element e_col = (Element) columnList.next();
									String name = e_col.elementTextTrim("Name");
									String columnCode = e_col.elementTextTrim("Code");
									String dataType = e_col.elementTextTrim("DataType");
									
									System.out.println(name+"-"+columnCode+"-"+dataType);
								}
								
								//生成文件
								writeFile(tableZNName, tableCNName, list);
								//写sql
								createSQL(bout, tableZNName, tableCNName, list);
								
								successTable += tableZNName + ",";
							}
						}
					}catch(Exception e) {
						try {
							in.close();
							readBase.close();
							readDetail.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			if(StringUtils.isNotBlank(successTable)) {
				successTable = successTable.substring(0, successTable.length() - 1);
			}
			System.out.println("生成成功："+successTable);
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			try {
				bout.flush();
				bout.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 生成文件
	 * @param tableZnName
	 * @param tableCnName
	 * @param columnList
	 * @throws Exception
	 */
	public static void writeFile(String tableZnName, String tableCnName, List columnList) throws Exception {
		
		//entity
		String entityName = getEntityName(tableZnName);//类名
		File entityFile = createCodeFile(filePath, entityName+".java");//文件
		writeEntityFile(entityFile, tableZnName, tableCnName, entityName, columnList);
		
		//dao
		String daoName = entityName+className_dao;
		File daoFile = createCodeFile(filePath, daoName+".java");
		writeDaoFile(daoFile, tableCnName, daoName, entityName);
		
		//daoImpl
		String daoImplName = entityName + className_dao_impl;
		File daoImplFile = createCodeFile(filePath, daoImplName+".java");
		writeDaoImplFile(daoImplFile, tableCnName, daoImplName, daoName, entityName);
		
		//service
		String serviceName = entityName + className_service;
		File serviceFile = createCodeFile(filePath, serviceName+".java");
		writeServiceFile(serviceFile, tableCnName, serviceName, entityName);
		
		//serviceImpl
		String serviceImplName = entityName + className_service_impl;
		File serviceImplFile = createCodeFile(filePath, serviceImplName+".java");
		writeServiceImplFile(serviceImplFile, tableCnName, serviceImplName, serviceName, daoName, entityName);
	}
	
	/**
	 * 写serviceImpl
	 * @param file
	 * @param tableCnName
	 * @param className
	 * @param entityName
	 */
	public static void writeServiceImplFile(File file, String tableCnName, String className, String serviceName, String daoName, String entityName) {
		if(file != null && file.exists()) {
			FileOutputStream out = null;
			BufferedOutputStream bout = null;
			try {
				out = new FileOutputStream(file);
				bout = new BufferedOutputStream(out);
				
				String reServiceName = serviceName.substring(0, 1).toLowerCase() + serviceName.substring(1);
				String reDaoName = daoName.substring(0, 1).toLowerCase() + daoName.substring(1);
				
				StringBuilder coding = new StringBuilder();//文件主体内容
				coding.append("package "+PackageName+"."+classPackage_service+"."+moduleName+".impl;\r\n \r\n");
				coding.append("import javax.annotation.Resource;\r\n" + 
						"import org.springframework.stereotype.Service;\r\n" + 
						"import org.springframework.transaction.annotation.Transactional;\r\n" + 
						"import "+PackageName+"."+classPackage_dao+"."+moduleName+"."+daoName+";\r\n" + 
						"import "+PackageName+"."+classPackage_entity+"."+moduleName+"."+entityName+";\r\n" + 
						"import "+PackageName+"."+classPackage_service+"."+moduleName+"."+serviceName+";\r\n" + 
						"import "+baseServiceImpl+";\r\n \r\n");
				coding.append("/**\r\n" + 
						" *@className "+tableCnName+"service实现类\r\n" + 
						" * \r\n" + 
						" *@author "+author+"\r\n" + 
						" *@createDate "+nowDate+"\r\n" + 
						" */\r\n");
				coding.append("@Transactional\r\n" + 
						"@Service(\""+reServiceName+"\")\r\n" + 
						"public class "+className+" extends BaseServiceImpl<"+entityName+"> implements "+serviceName+" {\r\n" + 
						"\r\n" + 
						"	"+daoName+" "+reDaoName+";\r\n" + 
						"\r\n" + 
						"	@Resource(name = \""+reDaoName+"\")\r\n" + 
						"	public void set"+daoName+"("+daoName+" "+reDaoName+") {\r\n" + 
						"		this."+reDaoName+" = "+reDaoName+";\r\n" + 
						"		super.setBaseDao("+reDaoName+");\r\n" + 
						"	}\r\n" + 
						"\r\n" + 
						"}");
				
				System.out.println("serviceImpl开始写入=====================");
				bout.write(coding.toString().getBytes());
				bout.flush();
				System.out.println("serviceImpl写入完成=====================");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bout.flush();
					bout.close();
					out.close();
				}catch(Exception e) {
					
				}
			}
		}
	}
	
	/**
	 * 写service
	 * @param file
	 * @param tableCnName
	 * @param className
	 * @param entityName
	 */
	public static void writeServiceFile(File file, String tableCnName, String className, String entityName) {
		if(file != null && file.exists()) {
			FileOutputStream out = null;
			BufferedOutputStream bout = null;
			try {
				out = new FileOutputStream(file);
				bout = new BufferedOutputStream(out);
				
				StringBuilder coding = new StringBuilder();//文件主体内容
				coding.append("package "+PackageName+"."+classPackage_service+"."+moduleName+";\r\n \r\n");
				coding.append("import "+PackageName+"."+classPackage_entity+"."+moduleName+"."+entityName+";\r\n" + 
						"import "+baseService+";\r\n \r\n");
				coding.append("/**\r\n" + 
						" *@className "+tableCnName+"service\r\n" + 
						" * \r\n" + 
						" *@author "+author+"\r\n" + 
						" *@createDate "+nowDate+"\r\n" + 
						" */\r\n");
				coding.append("public interface "+className+" extends BaseService<"+entityName+"> {\r\n \r\n }");
				
				System.out.println("service开始写入=====================");
				bout.write(coding.toString().getBytes());
				bout.flush();
				System.out.println("service写入完成=====================");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bout.flush();
					bout.close();
					out.close();
				}catch(Exception e) {
					
				}
			}
		}
	}
	
	/**
	 * 写daoImpl
	 * @param file
	 * @param tableCnName
	 * @param className
	 * @param daoName
	 * @param entityName
	 */
	public static void writeDaoImplFile(File file, String tableCnName, String className, String daoName, String entityName) {
		if(file != null && file.exists()) {
			FileOutputStream out = null;
			BufferedOutputStream bout = null;
			try {
				out = new FileOutputStream(file);
				bout = new BufferedOutputStream(out);
				
				String reDaoName = daoName.substring(0, 1).toLowerCase() + daoName.substring(1);

				StringBuilder coding = new StringBuilder();//文件主体内容
				coding.append("package "+PackageName+"."+classPackage_dao+"."+moduleName+".impl;\r\n \r\n");
				coding.append("import org.springframework.stereotype.Repository;\r\n" + 
						"import "+PackageName+"."+classPackage_dao+"."+moduleName+"."+daoName+";\r\n" + 
						"import "+PackageName+"."+classPackage_entity+"."+moduleName+"."+entityName+";\r\n" + 
						"import "+baseDaoImpl+";\r\n \r\n");
				coding.append("/**\r\n" + 
						" *@className "+tableCnName+"dao实现类\r\n" + 
						" * \r\n" + 
						" *@author "+author+"\r\n" + 
						" *@createDate "+nowDate+"\r\n" + 
						" */\r\n");
				coding.append("@Repository(\""+reDaoName+"\")\r\n" + 
						"public class "+className+" extends BaseDaoImpl<"+entityName+"> implements "+daoName+" {\r\n \r\n }");
				
				System.out.println("daoImpl开始写入=====================");
				bout.write(coding.toString().getBytes());
				bout.flush();
				System.out.println("daoImpl写入完成=====================");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bout.flush();
					bout.close();
					out.close();
				}catch(Exception e) {
					
				}
			}
		}
	}
	
	/**
	 * 写dao
	 * @param file			写入文件
	 * @param tableCnName	表名
	 * @param className		dao类名
	 * @param entityName	实体名
	 */
	public static void writeDaoFile(File file, String tableCnName, String className, String entityName) {
		if(file != null && file.exists()) {
			FileOutputStream out = null;
			BufferedOutputStream bout = null;
			try {
				out = new FileOutputStream(file);
				bout = new BufferedOutputStream(out);

				StringBuilder coding = new StringBuilder();//文件主体内容
				coding.append("package "+PackageName+"."+classPackage_dao+"."+moduleName+";\r\n \r\n");
				coding.append("import "+PackageName+"."+classPackage_entity+"."+moduleName+"."+entityName+";\r\n" + 
						"import "+baseDao+";\r\n \r\n");
				coding.append("/**\r\n" + 
						" *@className "+tableCnName+"dao\r\n" + 
						" * \r\n" + 
						" *@author "+author+"\r\n" + 
						" *@createDate "+nowDate+"\r\n" + 
						" */\r\n" + 
						"public interface "+className+" extends BaseDao<"+entityName+"> {\r\n \r\n }");
				
				System.out.println("dao开始写入=====================");
				bout.write(coding.toString().getBytes());
				bout.flush();
				System.out.println("dao写入完成=====================");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bout.flush();
					bout.close();
					out.close();
				}catch(Exception e) {
					
				}
			}
		}
	}
	
	
	/**
	 * 写entity
	 * @param file			写入文件
	 * @param tableZnName	表名
	 * @param tableCnName	表中文名
	 * @param className		entity类名
	 * @param columnList	字段
	 * @deprecated	默认实体父类会带有 id、createrId、createDate、modifyId、modifyDate、i_del_flag，所以生成的时候，需要去掉这几个
	 */
	public static void writeEntityFile(File file, String tableZnName, String tableCnName, String className, List columnList) {
		if(file != null && file.exists()) {
			FileOutputStream out = null;
			BufferedOutputStream bout = null;
			try {
				out = new FileOutputStream(file);
				bout = new BufferedOutputStream(out);

				StringBuilder funs = new StringBuilder();//get、set 方法
				StringBuilder coding = new StringBuilder();//文件主体内容
				coding.append("package "+PackageName+"."+classPackage_entity+"."+moduleName+";\r\n \r\n");
				coding.append("import java.math.BigDecimal;\r\n" + 
						"import java.util.Date;\r\n" + 
						"import javax.persistence.Column;\r\n" + 
						"import javax.persistence.Entity;\r\n" + 
						"import javax.persistence.Table;\r\n" + 
						"import "+baseEntity+";\r\n \r\n");
				coding.append("/**\r\n" + 
						" * @className "+tableCnName+"\r\n" + 
						" * \r\n" + 
						" * @author "+author+"\r\n" + 
						" * @createDate "+nowDate+"\r\n" + 
						" */\r\n" + 
						"@Entity\r\n" + 
						"@Table(name = \""+tableZnName+"\")\r\n" + 
						"public class "+className+" extends BaseDBEntity implements java.io.Serializable {\r\n" + 
						"\r\n" + 
						"	private static final long serialVersionUID = 1L;\r\n \r\n");
				
				Iterator columnIt = columnList.iterator();
				while (columnIt.hasNext()) {
					Element e_col = (Element) columnIt.next();
					String name = e_col.elementTextTrim("Name");
					String columnCode = e_col.elementTextTrim("Code");
					String dataType = e_col.elementTextTrim("DataType");
					
					//实体父类属性，自动跳过
					if(columnCode.indexOf("s_id") != -1 || 
							columnCode.indexOf("s_creater_id") != -1 || 
							columnCode.indexOf("d_create_date") != -1 || 
							columnCode.indexOf("s_modify_id") != -1 || 
							columnCode.indexOf("d_modify_date") != -1 ||
							columnCode.indexOf("i_del_flag") != -1) {
						continue;
					}
					
					coding.append("	private "+convertDataType(dataType)+" "+getColumnName(columnCode)+";//"+name+"\r\n");
					
					String cDataType = convertDataType(dataType);
					String columnName = getColumnName(columnCode);
					String funName = columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
					
					funs.append("	@Column(name = \""+columnCode+"\")\r\n" + 
							"	public "+cDataType+" get"+funName+"() {\r\n" + 
							"		return "+columnName+";\r\n" + 
							"	}\r\n" + 
							"	public void set"+funName+"("+cDataType+" "+columnName+") {\r\n" + 
							"		this."+columnName+" = "+columnName+";\r\n" + 
							"	} \r\n \r\n");
				}
				coding.append("\r\n");
				coding.append(funs);
				coding.append("\r\n}");
				
				System.out.println("entity开始写入=====================");
				bout.write(coding.toString().getBytes());
				bout.flush();
				System.out.println("entity写入完成=====================");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bout.flush();
					bout.close();
					out.close();
				}catch(Exception e) {
					
				}
			}
		}
	}
	
	
	/**
	 * 生成文件
	 * @param path 路径
	 * @param name 文件名
	 * @return
	 * @throws Exception
	 */
	public static File createCodeFile(String path, String name) throws Exception {
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdir();
		}
		File codeFile = new File(path + "/" + name);
		if(!codeFile.exists()) {
			codeFile.createNewFile();
		}
		return codeFile;
	}
	
	/**
	 * 根据表名,转换成实体名
	 * 注：表名必须以 T_ 开头，如：T_HOUSE_PRO_DETAIL
	 * @param tableName
	 * @return
	 */
	public static String getEntityName(String tableName) {
		String className = "";
		if(StringUtils.isNotBlank(tableName)) {
			tableName = tableName.substring(2);
			String[] tableNames = tableName.split("_");
			for(String t : tableNames) {
				className += t.substring(0, 1).toUpperCase() + t.substring(1, t.length()).toLowerCase();
			}
		}
		return className;
	}
	
	/**
	 * 根据字段名,转换成变量名
	 * 注：字段名开头必须为 类型_ ，如：s_name、i_type
	 * @param columnCode
	 * @return
	 */
	public static String getColumnName(String columnCode) {
		String columnName = "";
		if(StringUtils.isNotBlank(columnCode)) {
			columnCode = columnCode.substring(2);
			String[] columnCodes = columnCode.split("_");
			int i = 1;
			for(String s : columnCodes) {
				if(i == 1) {
					i++;
					columnName += s.toLowerCase();
				}else {
					columnName += s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
				}
			}
		}
		
		return columnName;
	}
	
	/**
	 * 根据表类型,转换成java类型
	 * 如：varchar -> String
	 * @param dataType
	 * @return
	 */
	public static String convertDataType(String dataType) {
		if(StringUtils.isNotBlank(dataType)) {
			if(dataType.indexOf("varchar") != -1) {
				return "String";
			}
			if(dataType.indexOf("numeric") != -1) {
				if(dataType.indexOf(",") != -1) {
					String num = dataType.substring(dataType.indexOf("(")+1, dataType.indexOf(")"));
					String[] nums = num.split(",");
					Integer n1 = Integer.parseInt(nums[0]);
					Integer n2 = Integer.parseInt(nums[1]);
					if(n2 > 2) {
						return "BigDecimal";
					}
					return "Double";
				}
				return "Integer";
			}
			if(dataType.indexOf("date") != -1) {
				return "Date";
			}
			if(dataType.indexOf("int") != -1) {
				return "Integer";
			}
		}
		
		return null;
	}
	
}
