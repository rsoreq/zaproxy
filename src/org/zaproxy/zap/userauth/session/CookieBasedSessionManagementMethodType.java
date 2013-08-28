package org.zaproxy.zap.userauth.session;

import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.httpsessions.ExtensionHttpSessions;
import org.zaproxy.zap.extension.httpsessions.HttpSession;
import org.zaproxy.zap.extension.httpsessions.HttpSessionTokensSet;
import org.zaproxy.zap.model.Context;

/**
 * The type corresponding to a {@link SessionManagementMethod} for web applications that use cookies
 * for session management.
 */
public class CookieBasedSessionManagementMethodType extends SessionManagementMethodType {

	private static final int METHOD_IDENTIFIER = 0;
	/** The Constant METHOD_NAME. */
	private static final String METHOD_NAME = Constant.messages.getString("sessionmanagement.method.cb.name");

	/**
	 * The implementation for a {@link SessionManagementMethod} that for web applications that use
	 * cookies for session management.
	 */
	public static class CookieBasedSessionManagementMethod implements SessionManagementMethod {

		private static final Logger log = Logger.getLogger(CookieBasedSessionManagementMethod.class);

		private int contextId;

		private Context context;

		private static WeakReference<ExtensionHttpSessions> extHttpSessions;

		public CookieBasedSessionManagementMethod(int contextId) {
			this.contextId = contextId;
		}

		@Override
		public String toString() {
			return CookieBasedSessionManagementMethodType.METHOD_NAME;
		}

		@Override
		public boolean isConfigured() {
			// Always configured
			return true;
		}

		@Override
		public WebSession extractWebSession(HttpMessage msg) {
			// TODO: Based on the already identified http session. This is based on the assumption
			// that the HttpSessions Extension has already identified the session
			return msg.getHttpSession();
			// return identifyMatchingWebSession(getSessionsForContext(), msg);
		}

		@Override
		public void processMessageToMatchSession(HttpMessage message, WebSession session)
				throws UnsupportedWebSessionException {

			if (session != null && message.getHttpSession() != session) {
				if (log.isDebugEnabled()) {
					log.debug("Modifying message to match session: " + session);
				}

				// make sure it's the right type
				if (!(session instanceof HttpSession)) {
					throw new UnsupportedWebSessionException(
							"The WebSession type provided is unsupported. Cookie based session management only supports "
									+ HttpSession.class + " type of WebSession.");
				}

				CookieBasedSessionManagementHelper.processMessageToMatchSession(message,
						(HttpSession) session);
			}
		}

		@Override
		public WebSession identifyMatchingWebSession(List<WebSession> sessions, HttpMessage msg)
				throws UnsupportedWebSessionException {
			// TODO: Proper implementation. Now it's hacked and does not work in all scenarios
			return CookieBasedSessionManagementHelper.getMatchingHttpSession(getSessionsForContext(), msg
					.getRequestHeader().getHttpCookies(), getHttpSessionsExtension()
					.getHttpSessionTokensSetForContext(getContext()));
		}

		private ExtensionHttpSessions getHttpSessionsExtension() {
			if (extHttpSessions == null || extHttpSessions.get() == null) {
				extHttpSessions = new WeakReference<ExtensionHttpSessions>((ExtensionHttpSessions) Control
						.getSingleton().getExtensionLoader().getExtension(ExtensionHttpSessions.class));
				if (extHttpSessions == null)
					log.error("An error occured while loading the ExtensionHttpSessions.");
			}
			return extHttpSessions.get();
		}

		private Context getContext() {
			if (context == null) {
				context = Model.getSingleton().getSession().getContext(contextId);
			}
			return context;
		}

		private List<HttpSession> getSessionsForContext() {
			return getHttpSessionsExtension().getHttpSessionsForContext(getContext());
		}

		@Override
		public SessionManagementMethod duplicate() {
			return new CookieBasedSessionManagementMethod(contextId);
		}

		@Override
		public void clearWebSessionIdentifiers(HttpMessage msg) {
			HttpSessionTokensSet tokens = getHttpSessionsExtension().getHttpSessionTokensSetForContext(
					getContext());
			if (tokens == null) {
				log.info("No tokens to clear.");
				return;
			}
			List<HttpCookie> requestCookies = msg.getRequestHeader().getHttpCookies();
			Iterator<HttpCookie> it = requestCookies.iterator();
			while (it.hasNext())
				if (tokens.isSessionToken(it.next().getName()))
					it.remove();
			msg.setCookies(requestCookies);
		}

		@Override
		public SessionManagementMethodType getType() {
			return new CookieBasedSessionManagementMethodType();
		}
	}

	@Override
	public CookieBasedSessionManagementMethod createSessionManagementMethod(int contextId) {
		return new CookieBasedSessionManagementMethod(contextId);
	}

	@Override
	public String getName() {
		return METHOD_NAME;
	}

	@Override
	public AbstractSessionManagementMethodOptionsPanel buildOptionsPanel(Context uiSharedContext) {
		// No need for a configuration panel yet
		return null;
	}

	@Override
	public boolean hasOptionsPanel() {
		return false;
	}

	@Override
	public boolean isTypeForMethod(SessionManagementMethod method) {
		return (method instanceof CookieBasedSessionManagementMethod);
	}

	@Override
	public void hook(ExtensionHook extensionHook) {
		// No need for hooking anything
	}

	@Override
	public int getUniqueIdentifier() {
		return METHOD_IDENTIFIER;
	}

	@Override
	public SessionManagementMethod loadMethodFromSession(Session session, int contextId) throws SQLException {
		return new CookieBasedSessionManagementMethod(contextId);
	}

	@Override
	public void persistMethodToSession(Session session, int contextId, SessionManagementMethod method)
			throws UnsupportedSessionManagementMethodException, SQLException {
		// Nothing to persist

	}
}