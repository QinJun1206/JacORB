package test.problems.typecode;

public class ServerOB
{
    static int
    run(org.omg.CORBA.ORB orb, String[] args)
	throws org.omg.CORBA.UserException
    {
	//
	// Resolve Root POA
	//
	org.omg.PortableServer.POA rootPOA =
	    org.omg.PortableServer.POAHelper.narrow(
		orb.resolve_initial_references("RootPOA"));

	//
	// Get a reference to the POA manager
	//
	org.omg.PortableServer.POAManager manager = rootPOA.the_POAManager();
	manager.activate();

	//
	// Create implementation object
	//
	Transferable_impl transferableImpl = new Transferable_impl(rootPOA);
	Transferable transferable = transferableImpl._this(orb);
//	org.omg.CORBA.Object transferable = rootPOA.servant_to_reference(transferableImpl);

	//
	// Save reference
	//
	try
	{
	    String ref = orb.object_to_string(transferable);
	    String refFile = "Transferable.ref";
	    java.io.FileOutputStream file = new java.io.FileOutputStream(refFile);
	    java.io.PrintWriter out = new java.io.PrintWriter(file);
	    out.println(ref);
	    out.flush();
	    file.close();
	}
	catch(java.io.IOException ex)
	{
	    System.err.println("hello.Server: can't write to `" +
                               ex.getMessage() + "'");
	    return 1;
	}

   System.out.println("Ready ...");
	//
	// Run implementation
	//
	orb.run();

	return 0;
    }

    public static void
    main(String args[])
    {
        java.util.Properties props = System.getProperties();
          props.put("org.omg.CORBA.ORBClass", "com.ooc.CORBA.ORB");
          props.put("org.omg.CORBA.ORBSingletonClass", "com.ooc.CORBA.ORBSingleton");
        


	int status = 0;
	org.omg.CORBA.ORB orb = null;

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
//  	    try
//  	    {
//                  		((com.ooc.CORBA.ORB)orb).destroy();
//  	    }
//  	    catch(Exception ex)
//  	    {
//  		ex.printStackTrace();
//  		status = 1;
//  	    }
	}

	System.exit(status);
    }
}
