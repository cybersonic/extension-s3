package org.lucee.extension.resource.s3;

public class S3Properties {

	private String host = S3.DEFAULT_HOST;
	private String secretAccessKey;
	private String accessKeyId;
	private boolean hasCustomCredentials;
	private Object acl;

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}

	public String getSecretAccessKey() {
		return secretAccessKey;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setCustomCredentials(boolean hasCustomCredentials) {
		this.hasCustomCredentials = hasCustomCredentials;
	}

	public boolean getCustomCredentials() {
		return hasCustomCredentials;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("host:").append(host).append(";").append("accessKeyId:").append(accessKeyId).append(";").append("secretAccessKey:")
				.append(secretAccessKey).append(";").append("custom:").append(hasCustomCredentials).append(";").toString();
	}

	public void setACL(Object acl) {
		this.acl = acl;
	}

	public Object getACL() {
		return this.acl;
	}
}
