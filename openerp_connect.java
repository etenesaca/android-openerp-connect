package com.openerp.attendances;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.content.ContentValues;
import android.util.Log;

/**
 * This class provides access to basic methods in OpenObject, so you can use
 * them from an Android device. The operations supported are: <br>
 * <ul>
 * <li>login</li>
 * <li>create</li>
 * <li>search</li>
 * <li>read</li>
 * <li>write</li>
 * <li>unlink</li>
 * <li>browse</li>
 * <li>call (This is a generic method to call whatever you need)</li>
 * </ul>
 * You can extend OpenErpConnect to implement more specific methods of your
 * need.
 * 
 * @author Enric Caumons Gou <caumons@gmail.com>
 * */
public class OpenErpConnect {

	protected String mServer;
	protected Integer mPort;
	protected String mDatabase;
	protected String mUserName;
	protected String mPassword;
	private Integer mUserId;
	protected URL mUrl;

	protected static final String CONNECTOR_NAME = "OpenErpConnect";

	public Integer getUserId() {
		return mUserId;
	}

	public void setUserId(Integer mUserId) {
		this.mUserId = mUserId;
	}

	public String getServer() {
		return mServer;
	}

	public void setServer(String mServer) {
		this.mServer = mServer;
	}

	public Integer getPort() {
		return mPort;
	}

	public void setPort(Integer mPort) {
		this.mPort = mPort;
	}

	public String getDatabase() {
		return mDatabase;
	}

	public void setDatabase(String mDatabase) {
		this.mDatabase = mDatabase;
	}

	public String getUserName() {
		return mUserName;
	}

	public void setUserName(String mUserName) {
		this.mUserName = mUserName;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setPassword(String mPassword) {
		this.mPassword = mPassword;
	}

	/** You should not use the constructor directly, use connect() instead */
	public OpenErpConnect(String server, Integer port, String db, String user, String pass, Integer id) throws MalformedURLException {
		mServer = server;
		mPort = port;
		mDatabase = db;
		mUserName = user;
		mPassword = pass;
		mUserId = id;
		mUrl = new URL("http", server, port, "/xmlrpc/object");
	}

	/**
	 * @return An OpenErpConnect instance, which you will use to call the
	 *         methods.
	 */
	public static OpenErpConnect connect(String server, Integer port, String db, String user, String pass) {
		return login(server, port, db, user, pass);
	}

	public static OpenErpConnect connect(ContentValues connectionParams) {
		return login(connectionParams);
	}

	protected static OpenErpConnect login(ContentValues connectionParams) {
		return login(connectionParams.getAsString("server"), connectionParams.getAsInteger("port"), connectionParams.getAsString("database"), connectionParams.getAsString("username"),
				connectionParams.getAsString("password"));
	}

	protected static OpenErpConnect login(String server, Integer port, String db, String user, String pass) {
		OpenErpConnect connection = null;
		try {
			URL loginUrl = new URL("http", server, port, "/xmlrpc/common");
			XMLRPCClient client = new XMLRPCClient(loginUrl);
			Integer id = (Integer) client.call("login", db, user, pass);
			connection = new OpenErpConnect(server, port, db, user, pass, id);
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		} catch (MalformedURLException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		} catch (ClassCastException e) {
			Log.d(CONNECTOR_NAME, e.toString()); // Bad login or password
		}
		return connection;
	}

	/**
	 * Creates a new record for the given model width the values supplied, if
	 * you do not need the context, just pass null for it. Remember: In order to
	 * add different types in a Collection use Object, e.g. <br>
	 * <code>
	 * HashMap<String, Object> values = new HashMap<String, Object>(); <br>
	 * values.put("name", "hello"); <br>
	 * values.put("number", 10); <br>
	 * </code>
	 * */
	public Long create(String model, HashMap<String, ?> values, HashMap<String, ?> context) {
		Long newObjectId = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			newObjectId = ((Integer) client.call("execute", mDatabase, getUserId(), mPassword, model, "create", values, context)).longValue();
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		}
		return newObjectId;
	}

	public Long[] search(String model, Object[] conditions) {
		return search(model, false, 0, 0, null, false, conditions);
	}

	public Long[] search(String model, Object[] conditions, Integer limit) {
		return search(model, false, 0, limit, null, false, conditions);
	}

	public Long[] search(String model, boolean count, Object[] conditions) {
		return search(model, count, 0, 0, null, false, conditions);
	}

	public Long[] search(String model, boolean count, Integer limit, String order, boolean reverseOrder, Object[] conditions) {
		return search(model, count, 0, limit, order, reverseOrder, conditions);
	}

	/**
	 * If count is true the resulting array will only contain the number of
	 * matching ids. You can pass new Object[0] to specify an empty list of
	 * conditions, which will return all the ids for that model.
	 * 
	 * @return The ids of matching objects.
	 * */
	public Long[] search(String model, boolean count, Integer offset, Integer limit, String order, boolean reverseOrder, Object[] conditions) {
		Long[] result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			List<Object> parameters = new ArrayList<Object>(11);
			parameters.add(mDatabase);
			parameters.add(getUserId());
			parameters.add(mPassword);
			parameters.add(model);
			parameters.add("search");
			parameters.add(conditions);
			parameters.add(offset);
			parameters.add(limit);
			parameters.add(order);
			parameters.add(null);
			parameters.add(count);
			if (count) { // We just want the number of items
				// result = new Long[] { ((Integer) client.call("execute",
				// parameters)).longValue() };
				result = new Long[] { ((Integer) client.call("execute", mDatabase, getUserId(), mPassword, model, "search", conditions, offset, limit)).longValue() };

			} else { // Returning the list of matching item id's
				Object[] responseIds = (Object[]) client.call("execute", mDatabase, getUserId(), mPassword, model, "search", conditions, offset, limit);
				// Object[] responseIds = (Object[]) client.call("execute",
				// parameters);

				// In case no matching records were found, an empty list is
				// returned by the ws
				// The ids are returned as Integer, but we want Long for better
				// Android compatibility
				result = new Long[responseIds.length];
				for (int i = 0; i < responseIds.length; i++) {
					result[i] = ((Integer) responseIds[i]).longValue();
				}
				if (reverseOrder) {
					reverseArray(result);
				}
			}
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		} catch (NullPointerException e) {
			Log.d(CONNECTOR_NAME, e.toString()); // Null response (should not
													// happen)
		}
		return result;
	}

	/**
	 * Each HashMap in the List contains the values for the specified fields for
	 * each object in the ids (in the same order).
	 * 
	 * @param fields
	 *            Specifying an empty fields array as: new String[0] will return
	 *            all the fields
	 * */
	public HashMap<String, Object> read(String model, long id, String[] fields) {
		HashMap<String, Object> Record = null;
		try {
			Long[] ids = { id };
			List<HashMap<String, Object>> records = read(model, ids, fields);
			Record = records.get(0);
		} catch (Exception e) {
		}
		return Record;
	}

	@SuppressWarnings("unchecked")
	public List<HashMap<String, Object>> read(String model, Long[] ids, String[] fields) {
		List<HashMap<String, Object>> Records = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object[] responseFields = (Object[]) client.call("execute", mDatabase, getUserId(), mPassword, model, "read", ids, fields);
			Records = new ArrayList<HashMap<String, Object>>(responseFields.length);
			for (Object objectFields : responseFields) {
				Records.add((HashMap<String, Object>) objectFields);
			}
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		}
		return Records;
	}

	/** Used to modify an existing object. */
	public Boolean write(String model, Long[] ids, HashMap<String, ?> values, HashMap<String, ?> context) {
		Boolean writeOk = false;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			writeOk = (Boolean) client.call("execute", mDatabase, getUserId(), mPassword, model, "write", ids, values, context);
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		}
		return writeOk;
	}

	/** A method to delete the matching records width the ids given */
	public Boolean unlink(String model, Long[] ids) {
		Boolean unlinkOk = false;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			unlinkOk = (Boolean) client.call("execute", mDatabase, getUserId(), mPassword, model, "unlink", ids);
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		}
		return unlinkOk;
	}

	/**
	 * The result is stored in the parameter List<E> resultList. The parameter
	 * modelClass should look like: MyClass.class Do not expect to use it as in
	 * the native method. You will not jump from one model to another just
	 * accessing the foreign field! But it is easier to work width E instances
	 * than HashMaps ;) The class E MUST define a public constructor with one
	 * parameter of type HashMap, which will initialize the attributes width the
	 * values inside the Hashmap width the keys corresponding to the fields
	 * supplied. It is recommended no to hardcode the fields, instead, program a
	 * public static method such as getAtrributeNames() in E that returns a
	 * List<String> width the attribute names in the OpenERP table, which match
	 * the attributes defined in the class. You can extend classes and call the
	 * parent's getAtrributeNames() to add() the new attributes (as it is a
	 * List<String>). Also, you can call the super constructor and populate just
	 * the new attributes. This may be useful for modules in OpenERP which add
	 * fields in existing models e.g. module MyModule adds the field
	 * my_module_field to res.partner, so you could define the classes
	 * ResPartner and ResPartnerMyModule, if needed. You can pass extras, which
	 * in turn will be received by the Class constructor in the form of
	 * "extra_0", "extra_1"... in the HashMap
	 * */
	public <E> void browse(String model, Class<E> modelClass, Long[] ids, List<String> fields, List<E> resultList, Object... extras) throws OpenErpConnectException {
		List<HashMap<String, Object>> listOfFieldValues = read(model, ids, fields.toArray(new String[fields.size()]));
		if (listOfFieldValues != null) {
			try {
				Constructor<E> constructor = modelClass.getConstructor(HashMap.class);
				for (HashMap<String, Object> objectHashmap : listOfFieldValues) {
					for (int numParam = 0; numParam < extras.length; numParam++) {
						objectHashmap.put("extra_" + numParam, extras[numParam]);
					}
					resultList.add(constructor.newInstance(objectHashmap));
				}
			} catch (SecurityException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			} catch (NoSuchMethodException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			} catch (IllegalArgumentException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			} catch (InstantiationException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			} catch (IllegalAccessException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			} catch (InvocationTargetException e) {
				Log.d(CONNECTOR_NAME, e.toString());
				throw new OpenErpConnectException(e.toString());
			}
		} else {
			throw new OpenErpConnectException(OpenErpConnectException.ERROR_READ);
		}
	}

	/**
	 * This is a generic method to call any WS.
	 * 
	 * @param parameters
	 *            Each one of the Objects can be one object instance, array or
	 *            List... depending on the WS called.
	 * */
	public Object call(String model, String method, Object... parameters) {
		Object response = null;
		try {
			List<Object> paramsList = new ArrayList<Object>(6);
			paramsList.add(mDatabase);
			paramsList.add(getUserId());
			paramsList.add(mPassword);
			paramsList.add(model);
			paramsList.add(method);
			for (Object parameter : parameters) {
				paramsList.add(parameter);
			}
			XMLRPCClient client = new XMLRPCClient(mUrl);
			response = client.call("execute", paramsList);
		} catch (XMLRPCException e) {
			Log.d(CONNECTOR_NAME, e.toString());
		}
		return response;
	}

	/**
	 * This utility method reverses the order of the Long elements (ids) in the
	 * array. Used to implement reverse ordering.
	 */
	public void reverseArray(Long[] array) {
		int minIndex = 0;
		int maxIndex = array.length - 1;
		long minValue;
		while (minIndex < maxIndex) {
			minValue = array[minIndex];
			array[minIndex] = array[maxIndex];
			array[maxIndex] = minValue;
			minIndex++;
			maxIndex--;
		}
	}

	/**
	 * @return String representation of the OpenErpConnection instance, good for
	 *         debugging purposes. You can comment the password if you want.
	 * */
	public String toString() {
		StringBuilder stringConn = new StringBuilder();
		stringConn.append("server: " + mServer + "\n");
		stringConn.append("port: " + mPort + "\n");
		stringConn.append("database: " + mDatabase + "\n");
		stringConn.append("user: " + mUserName + "\n");
		stringConn.append("password: " + mPassword + "\n");
		stringConn.append("id: " + getUserId() + "\n");
		return stringConn.toString();
	}

	/**
	 * As Java does not support output parameters; to control whether an
	 * Exception occurred in browse() method, we create this class, so the
	 * caller does not have to deal with so different kind of exceptions.
	 * Instead, it will be notified if one (any kind) occurred.<br>
	 * We can not assign the output parameter to null, because it is passed as a
	 * reference by value, so the changes to the reference itself outside the
	 * method will not be seen; we can just modify the object, not the memory
	 * position it is pointing.<br>
	 * The rest of methods just return null in case of Exception, because it is
	 * more agile, so you do not have to use try catch every time. However, if
	 * you wish, you can use this class with the rest of methods.
	 */
	public static class OpenErpConnectException extends Exception {

		private static final String ERROR_READ = "read() method returned unexpected null value";

		/** Required because Exception implements Serializable interface */
		private static final long serialVersionUID = 1L;

		public OpenErpConnectException(String message) {
			super(message);
		}
	}

	/*
	 * ************************************************************************
	 * EXTENSION DE LA LIBRERIA
	 * ************************************************************************
	 */

	/*
	 * Metodo para cargar las base de datos de Un Sevidor de OpenERP
	 */
	public static String[] getDatabaseList(String server, int port) {
		String[] result = null;

		try {
			URL ServerUrl;
			ServerUrl = new URL("http", server, port, "/xmlrpc/db");
			XMLRPCClient client = new XMLRPCClient(ServerUrl);

			Object aux = client.call("list", new ArrayList<Object>());
			Object[] a = (Object[]) aux;
			String[] res = new String[a.length];
			for (int i = 0; i < a.length; i++) {
				if (a[i] instanceof String) {
					res[i] = (String) a[i];
				}
			}
			return res;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}

		if (result == null) {
			result = new String[0];
		}
		return result;
	}

	/*
	 * Metodo para probar la Conexion aun Servidor de OpenERP
	 */
	protected static boolean TestConnection_method(String server, int port) {
		boolean result = false;
		try {
			URL ServerUrl;
			ServerUrl = new URL("http", server, port, "/xmlrpc/common");
			XMLRPCClient client = new XMLRPCClient(ServerUrl);
			Object res = client.call("check_connectivity");
			result = Boolean.parseBoolean(res + "");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (XMLRPCException e) {
			e.printStackTrace();
			return false;
		}
		return result;
	}

	public static void TestConnection_execute(final String server, final int port) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				gl.connected = TestConnection_method(server, port);
			}
		});
		thread.start();
		long endTimeMillis = System.currentTimeMillis() + 2000;
		while (thread.isAlive()) {
			if (System.currentTimeMillis() > endTimeMillis) {
				gl.connected = false;
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException t) {
			}
		}
	}

	public static boolean TestConnection(String server, int port) {
		TestConnection_execute(server, port);
		return gl.connected;
	}

	// Verificar los datos antes de registrar entrada o salida
	public String ValidateRegister() {
		String result = "";
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object resp = client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "validate_register");
			result = resp + "";
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Verificar los datos antes de registrar entrada o salida
	public boolean Module_Installed(String module_name) {
		boolean result = false;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object resp = client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "module_installed", module_name);
			result = Boolean.parseBoolean(resp + "");
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Verificar los datos antes de registrar entrada o salida
	public boolean Register_Attendance(Integer employee_id) {
		boolean result = false;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object resp = client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "register_attendance", employee_id);
			result = Boolean.parseBoolean(resp + "");
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtenner los registro de asistencia
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getRegisters(String From, String To, int employee_id) {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object registers_result = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getRegistersbyDate", From, To, employee_id);
			result = (HashMap<String, Object>) registers_result;
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtener el rango de fechas - Hoy
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getRangeDates_today() {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object registers_result = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getRangeDates_today");
			result = hupernikao.ConvertDatetoString((HashMap<String, Object>) registers_result);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtener el rango de fechas - Ayer
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getRangeDates_yesterday() {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object registers_result = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getRangeDates_yesterday");
			result = hupernikao.ConvertDatetoString((HashMap<String, Object>) registers_result);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtener el rango de fechas - Esta semana
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getRangeDates_this_week() {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object registers_result = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getRangeDates_this_week");
			result = hupernikao.ConvertDatetoString((HashMap<String, Object>) registers_result);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtener el rango de fechas - Esta semana
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getRangeDates_this_month() {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object registers_result = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getRangeDates_this_month");
			result = hupernikao.ConvertDatetoString((HashMap<String, Object>) registers_result);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Obtener el rango de fechas - Esta semana
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> getLastRegisterToday(int employeeID) {
		HashMap<String, Object> result = null;
		try {
			XMLRPCClient client = new XMLRPCClient(mUrl);
			Object last_register = (Object) client.call("execute", mDatabase, getUserId(), mPassword, "control.horario.register", "getLastRegisterToday", employeeID);
			result = (HashMap<String, Object>) last_register;
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;
	}
}