package org.lucee.extension.resource.s3.function;

import org.lucee.extension.resource.s3.S3;
import org.lucee.extension.resource.s3.S3Properties;
import org.lucee.extension.resource.s3.S3Resource;
import org.lucee.extension.resource.s3.S3ResourceProvider;

import lucee.commons.lang.types.RefString;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Cast;

public class S3GeneratePresignedURL extends S3Function {

	private static final long serialVersionUID = 1L;

	public static String call(PageContext pc, String bucketNameOrPath, String objectName, DateTime expireDate, String accessKeyId, String secretAccessKey, double timeout)
			throws PageException {
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		try {
			// create S3 Instance
			S3 s3 = S3ResourceProvider.getS3(toS3Properties(pc, accessKeyId, secretAccessKey), toTimeout(timeout));

			// get bucket and object from path
			if (Util.isEmpty(objectName) && ("" + bucketNameOrPath).toLowerCase().startsWith("s3://")) {
				S3Properties props = new S3Properties();
				RefString location = eng.getCreationUtil().createRefString(null);
				String[] bo = S3Resource.toBO(S3ResourceProvider.loadWithNewPattern(props, location, bucketNameOrPath.substring(5), Util.isEmpty(accessKeyId)));
				bucketNameOrPath = bo[0];
				objectName = bo[1];
				if (objectName != null && objectName.endsWith("/")) objectName = objectName.substring(0, objectName.length() - 1);
			}

			return s3.generatePresignedURL(bucketNameOrPath, objectName, expireDate).toExternalForm();
		}
		catch (Exception e) {
			throw eng.getCastUtil().toPageException(e);
		}
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Cast cast = engine.getCastUtil();

		if (args.length == 6) {
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), args[2] == null ? null : cast.toDateTime(args[2], pc.getTimeZone()), cast.toString(args[3]),
					cast.toString(args[4]), cast.toDoubleValue(args[5]));
		}
		if (args.length == 5) {
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), args[2] == null ? null : cast.toDateTime(args[2], pc.getTimeZone()), cast.toString(args[3]),
					cast.toString(args[4]), 0);
		}
		if (args.length == 4) {
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), args[2] == null ? null : cast.toDateTime(args[2], pc.getTimeZone()), cast.toString(args[3]), null, 0);
		}
		if (args.length == 3) {
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), args[2] == null ? null : cast.toDateTime(args[2], pc.getTimeZone()), null, null, 0);
		}
		if (args.length == 2) {
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), null, null, null, 0);
		}
		if (args.length == 1) {
			return call(pc, cast.toString(args[0]), null, null, null, null, 0);
		}

		throw engine.getExceptionUtil().createFunctionException(pc, "S3GeneratePresignedURL", 1, 6, args.length);
	}
}