package test.POA.local;

import org.omg.PortableServer.*;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

public class FooServantLocatorImpl 
    extends ServantLocatorPOA 
{
    private FooImpl foo = new FooImpl();
 
    public void postinvoke(byte[] oid, 
                           POA adapter, 
                           String operation, 
                           java.lang.Object cookie, 
                           Servant servant) 
    {
        String oidStr = new String(oid);
        if (!oidStr.equals(cookie)) 
        {
            System.out.println("[ postinvoke "+operation+" for oid: "+oidStr+": cookie is unknown ]");		
            throw new org.omg.CORBA.OBJECT_NOT_EXIST();
        }
    }

    public Servant preinvoke(byte[] oid, 
                             POA adapter, 
                             String operation, 
                             CookieHolder cookie) 
        throws ForwardRequest 
    {
        String oidStr = new String(oid);
        int oidInt = Integer.parseInt(oidStr);
        cookie.value = oidStr;
        return foo;
    }
}
