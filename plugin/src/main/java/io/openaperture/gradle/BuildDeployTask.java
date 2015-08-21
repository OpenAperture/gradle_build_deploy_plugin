package io.openaperture.gradle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.json.JSONObject;

public class BuildDeployTask extends DefaultTask {

	private String _deployRepo;
	private String _deployRepoGitRef = "master";
	private String _sourceRepoGitRef = "master";
	private String _oaUrl;
	private String _authUrl;
	private String _authUsername;
	private String _authPassword;
	private boolean _build = true;
	private boolean _deploy = true;
	private String _authToken = null;

	@TaskAction
	public void buildDeployTask() throws IOException {
		if (_authToken == null) {
			_authToken = doAuth();
			System.out.println("Auth attempt to " + _authUrl + " returned: " + _authToken);
		}
		String workflowId = createWorkflow(_authToken);
		System.out.println("Created workflow: " + workflowId);
		executeWorkflow(workflowId, _authToken);
		System.out.println("Workflow " + workflowId + " started for " + _deployRepo);
	}

	private void executeWorkflow(String workflowId, String token) throws ClientProtocolException, IOException {
		List<List<String>> payload = new ArrayList<List<String>>();
		payload.add(Arrays.asList(new String[] { "force_build", "false" }));
		HttpResponse response = post(payload, _oaUrl + "/workflows/" + workflowId + "/execute", token);
		if (response.getStatusLine().getStatusCode() != 202)
			throw new RuntimeException("Invalid status code returned executing workflow: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
	}

	private String doAuth() throws IOException {
		System.out.println("Submitting auth request to " + _authUrl);
		List<List<String>> payload = new ArrayList<List<String>>();
		payload.add(Arrays.asList(new String[] { "grant_type", "client_credentials" }));
		payload.add(Arrays.asList(new String[] { "client_id", _authUsername }));
		payload.add(Arrays.asList(new String[] { "client_secret", _authPassword }));
		HttpResponse response = post(payload, _authUrl, null);
		if (response.getStatusLine().getStatusCode() != 200)
			throw new RuntimeException("Invalid return code from auth server: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
		return getValueFromResponse(response, "access_token");
	}

	private static String getValueFromResponse(HttpResponse response, String string) throws UnsupportedOperationException, IOException {
		HttpEntity entity = response.getEntity();
		String ret = null;
		if (entity != null) {
			InputStream instream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
			try {
				String line = null;
				StringBuffer sb = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				ret = sb.toString();
			} finally {
				instream.close();
				reader.close();
			}
		}
		if (ret == null)
			throw new RuntimeException("Empty return from request");
		// System.out.println("ret: " + ret);
		return new JSONObject(ret).getString(string);
	}

	private static HttpResponse post(List<List<String>> payload, String url, String token) throws ClientProtocolException, IOException {
		// System.out.println("payload: " + payload);
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(url);
		if (token != null) {
			httppost.addHeader("Authorization", "Bearer access_token=" + token);
			// System.out.println("setting auth token: " + token);
		}
		StringEntity input = new StringEntity(buildPayload(payload));
		input.setContentType("application/json");
		httppost.setEntity(input);
		return httpclient.execute(httppost);
	}

	private static String buildPayload(List<List<String>> payload) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (List<String> part : payload) {
			if (first)
				first = false;
			else
				sb.append(",");
			sb.append("\"").append(part.get(0)).append("\":");
			if (!part.get(1).startsWith("["))
				sb.append("\"");
			sb.append(part.get(1));
			if (!part.get(1).startsWith("["))
				sb.append("\"");
		}
		sb.append("}");
		// System.out.println("payload:" + sb.toString());
		return sb.toString();
	}

	private String createWorkflow(String token) throws ClientProtocolException, IOException {
		System.out.println("Creating workflow. deploy repo: " + _deployRepo + ", deploy ref: " + _deployRepoGitRef + ", source ref: " + _sourceRepoGitRef + ", oaUrl: " + _oaUrl);

		List<List<String>> payload = new ArrayList<List<String>>();
		payload.add(Arrays.asList(new String[] { "deployment_repo", _deployRepo }));
		payload.add(Arrays.asList(new String[] { "deployment_repo_git_ref", _deployRepoGitRef }));
		payload.add(Arrays.asList(new String[] { "source_repo_git_ref", _sourceRepoGitRef }));

		String milestones = "";
		if (_build && _deploy)
			milestones = "[\"build\", \"deploy\"]";
		else if (_deploy)
			milestones = "[\"deploy\"]";
		else
			milestones = "[\"build\"]";

		payload.add(Arrays.asList(new String[] { "milestones", milestones }));

		HttpResponse response = post(payload, _oaUrl + "/workflows", token);
		if (response.getStatusLine().getStatusCode() != 201)
			throw new RuntimeException("Invalid status code returned creating workflow: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
		return afterLastSlash(response.getFirstHeader("location").getValue());
	}

	private static String afterLastSlash(String value) {
		return value.substring(value.lastIndexOf("/") + 1);
	}

	public void deployRepo(String deployRepo) {
		this._deployRepo = deployRepo;
	}

	public void sourceRepoGitRef(String sourceRepoGitRef) {
		this._sourceRepoGitRef = sourceRepoGitRef;
	}

	public void deployRepoGitRef(String deployRepoGitRef) {
		this._deployRepoGitRef = deployRepoGitRef;
	}

	public void oaUrl(String oaUrl) {
		this._oaUrl = oaUrl;
	}

	public void authUrl(String authUrl) {
		this._authUrl = authUrl;
	}

	public void authUsername(String authUsername) {
		this._authUsername = authUsername;
	}

	public void authPassword(String authPassword) {
		this._authPassword = authPassword;
	}

}
