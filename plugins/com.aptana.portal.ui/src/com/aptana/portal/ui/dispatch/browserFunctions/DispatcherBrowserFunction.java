package com.aptana.portal.ui.dispatch.browserFunctions;

import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.mortbay.util.ajax.JSON;

import com.aptana.portal.ui.PortalUIPlugin;
import com.aptana.portal.ui.dispatch.BrowserInteractionRegistry;
import com.aptana.portal.ui.dispatch.BrowserNotifier;
import com.aptana.portal.ui.dispatch.IActionController;
import com.aptana.portal.ui.dispatch.IBrowserNotificationConstants;

/**
 * This class is the main functions dispatcher for all the registered IActionControllers.
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class DispatcherBrowserFunction extends BrowserFunction
{
	/**
	 * Constructor.
	 * 
	 * @param browser
	 * @param name
	 */
	public DispatcherBrowserFunction(Browser browser, String name)
	{
		super(browser, name);
	}

	/**
	 * This function should always get a single argument of a JSON request, which can be transformed into a {@link Map}
	 * by calling {@link JSON#parse(String)}.<br>
	 * The function will then analyze the request and dispatch the appropriate {@link IActionController}.<br>
	 * In case the action controller does not exist, or the requested action from this controller does not exist, the
	 * function returns an error status wrapped in a JSON string. <br>
	 * The target {@link IActionController} and its action decide whether to dispatch in a synchronous or asynchronous
	 * way.<br>
	 * The browser should call this function with a JSON string that is formed this way (arguments can be null):
	 * 
	 * <pre>
	 *   {controller:"controller_name", action:"action_name", arguments:{arguments as JSON}}
	 * Examples:
	 *   dispatch($H({controller:"portal.recentFiles", action:"getRecentFiles"}).toJSON());
	 *   dispatch($H({controller:"portal.recentFiles", action:"openRecentFiles", args:fileA.toJSON()}).toJSON());
	 *   dispatch($H({controller:"portal.recentFiles", action:"openRecentFiles", args:[fileA, fileB].toJSON()}).toJSON());
	 * </pre>
	 * 
	 * @see IBrowserNotificationConstants#JSON_ERROR
	 * @see IBrowserNotificationConstants#JSON_ERROR_WRONG_ARGUMENTS
	 * @see IBrowserNotificationConstants#JSON_ERROR_UNKNOWN_CONTROLLER
	 * @see IBrowserNotificationConstants#JSON_ERROR_UNKNOWN_ACTION
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" })
	public Object function(Object[] arguments)
	{
		if (arguments == null || arguments.length != 1 || arguments[0] == null)
		{
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_WRONG_ARGUMENTS,
					Messages.DispatcherBrowserFunction_wrongOrMissingArguments);
		}
		Object jsonObject = JSON.parse(arguments[0].toString());
		if (!(jsonObject instanceof Map))
		{
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_WRONG_ARGUMENTS,
					Messages.DispatcherBrowserFunction_expectedJSONMap);
		}
		Map event = (Map) jsonObject;
		String controllerID = (String) event.get(IBrowserNotificationConstants.DISPATCH_CONTROLLER);
		IActionController controller = BrowserInteractionRegistry.getInstance().getActionController(controllerID);
		if (controller == null)
		{
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_UNKNOWN_CONTROLLER,
					Messages.DispatcherBrowserFunction_unknownController + controllerID);
		}
		String action = (String) event.get(IBrowserNotificationConstants.DISPATCH_ACTION);
		if (action == null || !controller.hasAction(action))
		{
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_UNKNOWN_ACTION,
					Messages.DispatcherBrowserFunction_unknownAction + action);
		}
		Object args = event.get(IBrowserNotificationConstants.DISPATCH_ARGUMENTS);
		try
		{
			if (args != null)
			{
				if (!(args instanceof Map))
				{
					// parse it only if it's a plain String
					args = JSON.parse(args.toString());
				}
				if (!(args instanceof Object[]))
				{
					// Make sure we pass the argument in an Object array anyway.
					// This allows the JavaScript side to pass the arguments as an array,
					// or as a simple value.
					args = new Object[] { args };
				}
			}
		}
		catch (IllegalStateException ise)
		{
			PortalUIPlugin.logError("The dispatch arguments were probably not passed as a valid JSON." //$NON-NLS-1$
					+ " Please check your JavaScript code.", ise); //$NON-NLS-1$
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_WRONG_ARGUMENTS,
					ise.getMessage());
		}
		catch (Exception e)
		{
			PortalUIPlugin.logError(e);
			return BrowserNotifier.toJSONErrorNotification(IBrowserNotificationConstants.JSON_ERROR_WRONG_ARGUMENTS, e
					.getMessage());
		}
		// OK... Done with the checks. Now dispatch.
		return dispatch(controller, action, args);
	}

	/**
	 * Dispatch the action controller function in a <b>synchronous</b> way.<br>
	 * The action that is being dispatched can still create a Job that will run asynchronously and report back when
	 * ready. It's the responsible of the controller & action implementation to return a value immediately (such as
	 * {@link IBrowserNotificationConstants#JSON_OK}) and run a Job, or to return after a synchronous computation.
	 * 
	 * @param controller
	 * @param action
	 * @param arguments
	 * @return The result of this dispatch.
	 */
	protected Object dispatch(IActionController controller, String action, Object arguments)
	{
		return controller.invokeAction(action, arguments);
	}
}
