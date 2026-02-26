package org.wsitm.schemax.utils.json;

/**
 * 工具类异常
 *
 * @author wsitm
 */
public class JSONException extends RuntimeException
{
    private static final long serialVersionUID = 8247610319171014183L;

    public JSONException(Throwable e)
    {
        super(e.getMessage(), e);
    }

    public JSONException(String message)
    {
        super(message);
    }

    public JSONException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
