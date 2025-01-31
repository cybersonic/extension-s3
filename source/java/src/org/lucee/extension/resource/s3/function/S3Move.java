package org.lucee.extension.resource.s3.function;

import org.lucee.extension.resource.s3.S3;
import org.lucee.extension.resource.s3.S3ResourceProvider;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.Cast;

public class S3Move extends S3Function {

	private static final long serialVersionUID = 7678256258034826909L;

	public static Object call(PageContext pc, String srcBucketName, String srcObjectName, String trgBucketName, String trgObjectName, String accessKeyId, String secretAccessKey,
			double timeout) throws PageException {
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		if (Util.isEmpty(trgObjectName, true)) trgObjectName = srcObjectName;
		try {
			// create S3 Instance
			S3 s3 = S3ResourceProvider.getS3(toS3Properties(pc, accessKeyId, secretAccessKey), toTimeout(timeout));
			s3.moveObject(srcBucketName, srcObjectName, trgBucketName, trgObjectName, null, null);
		}
		catch (Exception e) {
			throw eng.getCastUtil().toPageException(e);
		}
		return null;
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Cast cast = engine.getCastUtil();

		if (args.length == 7) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), cast.toString(args[4]),
				cast.toString(args[5]), cast.toDoubleValue(args[6]));
		if (args.length == 6)
			return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), cast.toString(args[4]), cast.toString(args[5]), 0);
		if (args.length == 5) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), cast.toString(args[4]), null, 0);
		if (args.length == 4) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), cast.toString(args[3]), null, null, 0);
		if (args.length == 3) return call(pc, cast.toString(args[0]), cast.toString(args[1]), cast.toString(args[2]), null, null, null, 0);

		throw engine.getExceptionUtil().createFunctionException(pc, "S3Move", 3, 7, args.length);
	}
}