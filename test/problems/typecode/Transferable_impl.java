// **********************************************************************
//
// Generated by the ORBacus IDL to Java Translator
//
// Copyright (c) 2000
// Object Oriented Concepts, Inc.
// Billerica, MA, USA
//
// All Rights Reserved
//
// **********************************************************************

// Version: 4.0.5

package test.problems.typecode;

//
// IDL:Transferable:1.0
//
/***/

public class Transferable_impl extends TransferablePOA
{
    private org.omg.PortableServer.POA poa_;

    public Transferable_impl(org.omg.PortableServer.POA poa)
    {
        poa_ = poa;
    }

    public org.omg.PortableServer.POA _default_POA()
    {
        if(poa_ != null)
            return poa_;
        else
            return super._default_POA();
    }

    //
    // IDL:Transferable/export_data:1.0
    //

    public org.omg.CORBA.Any export_data(DataFlavor flavor)
    {
        // TODO: implement
        Property[] pa1 = new Property[0];
        Property[] pa2 = new Property[0];

        FeaturePortrayal fp = new FeaturePortrayal( pa1,  pa2);

        FeaturePortrayal[] fpa = new FeaturePortrayal[] { fp };

        org.omg.CORBA.Any any = org.omg.CORBA.ORB.init().create_any();

        FeaturePortrayalSeqHelper.insert(any, fpa);
        //        TransferData _r = new TransferData(flavor, any);

        // TransferData _r = new TransferData( any);
        return any;
    }
}


