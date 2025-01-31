package org.lucee.extension.resource.s3.function;

import org.lucee.extension.resource.s3.S3;
import org.lucee.extension.resource.s3.S3ResourceProvider;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.Cast;

public class S3Exists extends S3Function {

	private static final long serialVersionUID = -3512292354903523227L;

	public static boolean call(PageContext pc, String bucketName, String objectName, String accessKeyId, String secretAccessKey, double timeout) throws PageException {
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		try {
			// create S3 Instance
			S3 s3 = S3ResourceProvider.getS3(toS3Properties(pc, accessKeyId, secretAccessKey), toTimeout(timeout));

			if (Util.isEmpty(objectName)) return s3.exists(bucketName);
			return s3.exists(bucketName, objectName);
		}
		catch (Exception e) {
			throw eng.getCastUtil().toPageException(e);
		}
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Cast cast = engine.getCastUtil();

		if (args.length == 5) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), cast.toDoubleValue(args[4]));
		if (args.length == 4) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), 0);
		if (args.length == 3) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), null, 0);
		if (args.length == 2) return call(pc, cast.toString(args[0]), cast.toString(args[1]), null, null, 0);
		if (args.length == 1) return call(pc, cast.toString(args[0]), null, null, null, 0);

		throw engine.getExceptionUtil().createFunctionException(pc, "S3Exists", 1, 5, args.length);
	}
}