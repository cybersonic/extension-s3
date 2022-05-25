/**
 *
 * Copyright (c) 2015, Lucee Assosication Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package org.lucee.extension.resource.s3.acl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lucee.extension.resource.s3.S3;
import org.lucee.extension.resource.s3.S3Exception;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.EmailAddressGrantee;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Creation;
import lucee.runtime.util.Decision;
import lucee.runtime.util.Strings;

public class AccessControlListUtil {

	public static final short TYPE_GROUP = 1;
	public static final short TYPE_EMAIL = 2;
	public static final short TYPE_CANONICAL_USER = 4;

	private String id;
	private String displayName;
	private String permission;
	private String uri;
	private short type;
	private String email;

	/**
	 * @return the type
	 */
	public short getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(short type) {
		this.type = type;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the permission
	 */
	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "displayName:" + displayName + ";email:" + email + ";id:" + id + ";permission:" + permission + ";type:" + type + ";uri:" + uri;
	}

	public String hash() {
		try {
			return CFMLEngineFactory.getInstance().getSystemUtil().hashMd5(toString());
		}
		catch (IOException e) {
			return null;
		}
	}

	@Override
	public int hashCode() {
		return hash().hashCode();
	}

	public static ACLList toAccessControlList(Object objACL) throws S3Exception {

		if (objACL instanceof ACLList) return (ACLList) objACL;
		if (objACL instanceof CannedAccessControlList) return new ACLList((CannedAccessControlList) objACL);
		if (objACL instanceof AccessControlList) return new ACLList((AccessControlList) objACL);

		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Decision dec = engine.getDecisionUtil();
		Cast cast = engine.getCastUtil();

		// String
		if (dec.isSimpleValue(objACL)) {
			String str = cast.toString(objACL, "");
			ACLList acl = S3.toACL(str, null);
			if (acl == null) throw new S3Exception("invalid access control list definition [" + str + "]");
			return acl;
		}

		// Array
		if (dec.isCastableToArray(objACL)) {
			Array arr = cast.toArray(objACL, null);
			if (arr == null || arr.size() == 0) return null;
			AccessControlList acl = new AccessControlList();
			acl.grantAllPermissions(toGrantAndPermissions(arr));
			return new ACLList(acl);
		}

		// Struct
		if (dec.isCastableToStruct(objACL)) {
			Struct sct = cast.toStruct(objACL, null);
			if (sct == null || sct.size() == 0) return null;
			AccessControlList acl = new AccessControlList();
			acl.grantAllPermissions(new Grant[] { toGrantAndPermission(sct) });
			return new ACLList(acl);
		}

		throw new S3Exception("access control list must be an array or a string");
	}

	public static Grant[] toGrantAndPermissions(Object obj) throws S3Exception, PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		if (engine.getDecisionUtil().isArray(obj)) return toGrantAndPermissions(engine.getCastUtil().toArray(obj));

		Grant gap = toGrantAndPermission(engine.getCastUtil().toStruct(obj));
		return new Grant[] { gap };
	}

	public static Grant[] toGrantAndPermissions(Array arr) throws S3Exception {
		Cast caster = CFMLEngineFactory.getInstance().getCastUtil();
		if (arr == null) throw new S3Exception("ACL Object must be a Array of Structs");

		Struct sct;
		Iterator<Object> it = arr.valueIterator();
		List<Grant> acl = new ArrayList<Grant>();
		while (it.hasNext()) {
			sct = caster.toStruct(it.next(), null);
			if (sct == null) throw new S3Exception("ACL Object must be a Array of Structs");
			acl.add(toGrantAndPermission(sct));
		}
		return acl.toArray(new Grant[acl.size()]);
	}

	public static Grant toGrantAndPermission(Struct sct) throws S3Exception {
		// Permission
		Permission perm = toPermission(sct);

		// Group
		Grantee grantee = toGroupGrantee(sct);
		if (grantee != null) return new Grant(grantee, perm);

		// Email
		grantee = toEmailAddressGrantee(sct);
		if (grantee != null) return new Grant(grantee, perm);

		// Canonical
		grantee = toCanonicalGrantee(sct);
		if (grantee != null) return new Grant(grantee, perm);

		throw new S3Exception("missing Grantee definition");
	}

	public static Permission toPermission(Struct sct) throws S3Exception {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Object oPermission = sct.get(engine.getCastUtil().toKey("permission"), null);
		String permission = engine.getCastUtil().toString(oPermission, null);
		if (engine.getStringUtil().isEmpty(permission, true)) throw new S3Exception("missing permission definition");

		permission = permission.toUpperCase().trim();
		String p = permission;
		permission = removeWordDelimter(permission);

		if ("FULLCONTROL".equalsIgnoreCase(permission)) return Permission.FullControl;
		else if ("WRITEACP".equalsIgnoreCase(permission)) return Permission.WriteAcp;
		else if ("READACP".equalsIgnoreCase(permission)) return Permission.ReadAcp;
		else if ("WRITE".equalsIgnoreCase(permission)) return Permission.Write;

		else if ("READ".equalsIgnoreCase(permission)) return Permission.Read;
		else if ("PUBLIC-READ".equalsIgnoreCase(permission)) return Permission.Read;
		else if ("PUBLICREAD".equalsIgnoreCase(permission)) return Permission.Read;
		else if ("PUBLIC_READ".equalsIgnoreCase(permission)) return Permission.Read;

		else throw new S3Exception("invalid permission definition [" + p + "], valid permissions are [FULL_CONTROL, WRITE, WRITE_ACP, READ, READ_ACP]");

	}

	private static GroupGrantee toGroupGrantee(Struct sct) throws S3Exception {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Object oGroup = sct.get(engine.getCastUtil().toKey("group"), null);

		if (oGroup != null) {
			String group = engine.getCastUtil().toString(oGroup, null);

			if (group == null) throw new S3Exception("invalid object type for group definition");

			group = removeWordDelimter(group);
			if ("all".equalsIgnoreCase(group) || "allusers".equalsIgnoreCase(group)) return GroupGrantee.AllUsers;
			if ("authenticated".equalsIgnoreCase(group) || "AuthenticatedUser".equalsIgnoreCase(group) || "AuthenticatedUsers".equalsIgnoreCase(group))
				return GroupGrantee.AuthenticatedUsers;
			if ("logdelivery".equalsIgnoreCase(group)) return GroupGrantee.LogDelivery;
			throw new S3Exception("invalid group definition [" + group + "], valid group defintions are are " + "[all,authenticated,log_delivery]");
		}
		return null;
	}

	private static EmailAddressGrantee toEmailAddressGrantee(Struct sct) throws S3Exception {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Object oEmail = sct.get(engine.getCastUtil().toKey("email"), null);
		if (oEmail != null) {
			String email = engine.getCastUtil().toString(oEmail, null);
			if (email == null) throw new S3Exception("invalid object type for email definition");
			return new EmailAddressGrantee(removeWordDelimter(email));
		}
		return null;
	}

	private static CanonicalGrantee toCanonicalGrantee(Struct sct) throws S3Exception {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Object oId = sct.get(engine.getCastUtil().toKey("id"), null);
		if (oId != null) {
			String id = engine.getCastUtil().toString(oId, null);
			if (id == null) throw new S3Exception("invalid object type for id definition");
			CanonicalGrantee canonical = new CanonicalGrantee(id);
			String displayName = engine.getCastUtil().toString(sct.get(engine.getCastUtil().toKey("displayName"), null), null);
			if (!Util.isEmpty(displayName, true)) canonical.setDisplayName(displayName);
			return canonical;
		}
		return null;
	}

	public static Array toArray(List<Grant> grants) {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Struct sct;
		Array arr = engine.getCreationUtil().createArray();
		if (grants != null && grants.size() > 0) {
			for (Grant g: grants) {
				sct = toStruct(g.getGrantee());
				sct.setEL(engine.getCreationUtil().createKey("permission"), g.getPermission().toString());
				arr.appendEL(sct);
			}
		}
		return arr;
	}

	private static Struct toStruct(Grantee grantee) {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Creation creator = engine.getCreationUtil();

		Struct sct = creator.createStruct();
		sct.setEL(creator.createKey("id"), grantee.getIdentifier());

		// Group
		if (grantee instanceof GroupGrantee) {
			if (GroupGrantee.AllUsers.equals(grantee)) sct.setEL(creator.createKey("group"), "all");
			else if (GroupGrantee.AuthenticatedUsers.equals(grantee)) sct.setEL(creator.createKey("group"), "authenticated");
			else if (GroupGrantee.LogDelivery.equals(grantee)) sct.setEL(creator.createKey("group"), "log_delivery");
		}

		// E-Mail
		else if (grantee instanceof EmailAddressGrantee) {
			sct.setEL(creator.createKey("email"), grantee.getIdentifier());
		}

		// Canonical
		else if (grantee instanceof CanonicalGrantee) {
			sct.setEL(creator.createKey("displayName"), ((CanonicalGrantee) grantee).getDisplayName());
		}
		return sct;
	}

	private static String removeWordDelimter(String str) {
		Strings util = CFMLEngineFactory.getInstance().getStringUtil();
		str = util.replace(str, "_", "", false, false);
		str = util.replace(str, "-", "", false, false);
		str = util.replace(str, " ", "", false, false);
		return str;
	}
}