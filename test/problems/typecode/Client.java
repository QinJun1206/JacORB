package test.problems.typecode;

import java.io.*;

public class Client
{
    static int run(org.omg.CORBA.ORB orb, String[] args)
	throws org.omg.CORBA.UserException
    {
	//
	// Get "Transferable" object
	//
        String ior;
        try 
	{
            File f = new File("Transferable.ref");
            BufferedReader br = new BufferedReader( new FileReader( f ));
            ior = br.readLine();
            br.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return 1;
        }

	org.omg.CORBA.Object obj = orb.string_to_object(ior);
	if(obj == null)
	{
	    System.err.println("Transferable.Client: cannot read IOR from Transferable.ref");
	    return 1;
	}

	Transferable transferable = TransferableHelper.narrow(obj);

        DataFlavor flavor = new DataFlavor("any", new NameValuePair[0]);
	org.omg.CORBA.Any data = transferable.export_data(flavor);
        FeaturePortrayal[] fpa = FeaturePortrayalSeqHelper.extract( data );
        //        System.out.println("Scope: " + fpa[0].scope);
	return 0;
    }

    //
    // Standalone program initialization
    //
    public static void main(String args[])
    {
	int status = 0;
	org.omg.CORBA.ORB orb = null;

	java.util.Properties props = System.getProperties();
        /*
          props.put("org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
          props.put("org.omg.CORBA.ORBSingletonClass", "com.ooc.CORBA.ORBSingleton");
        */
        props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
        props.put("jacorb.verbosity", "8");

	try
	{
            orb = org.omg.CORBA.ORB.init(args, props);
	    status = run(orb, args);
	}
	catch(Exception ex)
	{
	    ex.printStackTrace();
	    status = 1;
	}

	if(orb != null)
	{
	    //
	    // Since the standard ORB.destroy() method is not present in
	    // JDK 1.2.x, we must cast to com.ooc.CORBA.ORB so that this
	    // will compile with all JDK versions
	    //
	    try
	    {
		orb.destroy();
	    }
	    catch(Exception ex)
	    {
		ex.printStackTrace();
		status = 1;
	    }
	}

	System.exit(status);
    }
}
