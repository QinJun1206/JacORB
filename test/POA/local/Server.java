package test.POA.local;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.*;
import org.omg.CosNaming.*;
import java.io.*;

public class Server 
{
    public static String factoryPOAName = "factoryPOA";
    public static String fooPOAName = "fooPOA";
    public static String description;

    public static void main(String[] args)  
    {
        try 
        {  
            ORB orb
                = org.omg.CORBA.ORB.init(args, null);
            POA rootPOA = 
                POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POAManager poaMgr  = rootPOA.the_POAManager();
		    
            // create a user defined poa for the foo factory
            org.omg.CORBA.Policy [] policies = {				
                rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID),
                rootPOA.create_lifespan_policy(LifespanPolicyValue.PERSISTENT)
            };

            POA factoryPOA = 
                rootPOA.create_POA(factoryPOAName, poaMgr, policies);

            for (int i=0; i<policies.length; i++) 
                policies[i].destroy();
			
            // implicit activation of an adpater activator on root poa
            //factoryPOA.the_activator(new FooAdapterActivatorImpl()._this(orb));
			
            // explicit activation of the factory servant on factory poa
            FooFactoryImpl factoryServant = new FooFactoryImpl( factoryPOA );

            factoryPOA.activate_object_with_id(new String("FooFactory").getBytes(), factoryServant);


            org.omg.CORBA.Policy [] foo_policies = {
                factoryPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_SERVANT_MANAGER),
                factoryPOA.create_servant_retention_policy(ServantRetentionPolicyValue.NON_RETAIN),
                factoryPOA.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID),
                factoryPOA.create_lifespan_policy(LifespanPolicyValue.PERSISTENT)
            };

            POA newPOA = 
                factoryPOA.create_POA( fooPOAName, 
                                       factoryPOA.the_POAManager(), 
                                       foo_policies);

            for (int i=0; i < foo_policies.length; i++) 
                foo_policies[i].destroy();
				
            newPOA.set_servant_manager( new FooServantLocatorImpl()._this( orb ));

			
            // register factory on name service 
            NamingContextExt nc =
                NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));


            nc.bind( nc.to_name("FooFactory.service") , factoryServant._this(orb) );

            // activate the poa manager
            poaMgr.activate();
            System.out.println("[ Server ready ]");			
            orb.run();
			
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
