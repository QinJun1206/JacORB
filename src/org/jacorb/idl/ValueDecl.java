/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.jacorb.idl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Andre Spiegel
 * @version $Id$
 */

class ValueDecl
    extends Value
{
    private MemberList stateMembers;
    private List operations;
    private List exports;
    private List factories;
    private ValueInheritanceSpec inheritanceSpec;

    private boolean isCustomMarshalled = false;

    public ValueDecl( int num )
    {
        super( num );
        stateMembers = new MemberList( new_num() );
        operations = new ArrayList();
        exports = new ArrayList();
        factories = new ArrayList();
    }

    public void setValueElements( Definitions d )
    {
        for( Iterator i = d.v.iterator(); i.hasNext(); )
        {
            Declaration dec = ( (Definition)( i.next() ) ).get_declaration();
            if( dec instanceof StateMember )
                stateMembers.v.add( dec );
            else if( dec instanceof OpDecl )
                operations.add( dec );
            else if( dec instanceof InitDecl )
                factories.add( dec );
            else
                exports.add( dec );
        }
        stateMembers.setContainingType( this );
        stateMembers.setPackage( name );
        if( stateMembers != null )
            stateMembers.setEnclosingSymbol( this );
    }

    public void setInheritanceSpec( ValueInheritanceSpec spec )
    {
        inheritanceSpec = spec;
    }

    public ValueInheritanceSpec setInheritanceSpec()
    {
        return inheritanceSpec;
    }

    public void isCustomMarshalled( boolean flag )
    {
        this.isCustomMarshalled = flag;
    }

    public boolean isCustomMarshalled()
    {
        return this.isCustomMarshalled;
    }

    public void setPackage( String s )
    {
        s = parser.pack_replace( s );
        if( pack_name.length() > 0 )
            pack_name = s + "." + pack_name;
        else
            pack_name = s;

        stateMembers.setPackage( s );

        if( inheritanceSpec != null )
            inheritanceSpec.setPackage( s );

        for( Iterator i = operations.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setPackage( s );

        for( Iterator i = exports.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setPackage( s );

        for( Iterator i = factories.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setPackage( s );
    }

    public TypeDeclaration declaration()
    {
        return this;
    }

    public void parse()
    {
        boolean justAnotherOne = false;

        if( isCustomMarshalled() &&
                inheritanceSpec != null &&
                inheritanceSpec.truncatable != null )
        {
            parser.error( "Valuetype " + typeName() +
                          " may no be BOTH custom AND truncatable", token );
        }

        ConstrTypeSpec ctspec = new ConstrTypeSpec( new_num() );

        try
        {
            escapeName();
            ScopedName.definePseudoScope( full_name() );

            ctspec.c_type_spec = this;

            NameTable.define( full_name(), "type" );
            TypeMap.typedef( full_name(), ctspec );
        }
        catch( NameAlreadyDefined nad )
        {
            if (parser.get_pending (full_name ()) != null)
            {
                if (stateMembers.size () != 0)
                {
                    justAnotherOne = true;
                }
                if( ! full_name().equals( "org.omg.CORBA.TypeCode" ) && stateMembers.size () != 0 )
                {
                    TypeMap.replaceForwardDeclaration( full_name(), ctspec );
                }
            }
            else
            {
                Environment.output( 4, nad );
                parser.error( "Valuetype " + typeName() + " already defined", token );
            }
        }

        if (stateMembers.size () != 0)
        {
            stateMembers.parse();

            for( Iterator i = operations.iterator(); i.hasNext(); )
                ( (IdlSymbol)i.next() ).parse();

            for( Iterator i = exports.iterator(); i.hasNext(); )
            {
                IdlSymbol sym = (IdlSymbol)i.next();
                sym.parse();
                if( sym instanceof AttrDecl )
                {
                    for( Enumeration e = ( (AttrDecl)sym ).getOperations();
                         e.hasMoreElements(); )
                        operations.add( e.nextElement() );
                }
            }

            // check inheritance rules

            if( inheritanceSpec != null )
            {
                Hashtable h = new Hashtable();
                for( Enumeration e = inheritanceSpec.getValueTypes();
                     e.hasMoreElements(); )
                {
                    ScopedName name = (ScopedName)e.nextElement();
                    ConstrTypeSpec ts =
                        (ConstrTypeSpec)name.resolvedTypeSpec().typeSpec();

                    if( ts.declaration() instanceof Value )
                    {
                        if( h.containsKey( ts.full_name() ))
                        {
                            parser.fatal_error( "Illegal inheritance spec: " +
                                                inheritanceSpec  +
                                                " (repeated inheritance not allowed).",
                                                token );
                        }
                        // else:
                        h.put( ts.full_name(), "" );
                        continue;
                    }
                    else
                    {
                        System.out.println( " Declaration is " + ts.declaration().getClass() );
                        parser.fatal_error( "Non-value type in inheritance spec: \n\t" +
                                            inheritanceSpec, token );
                    }
                }

                for( Enumeration e = inheritanceSpec.getSupportedInterfaces();
                     e.hasMoreElements(); )
                {
                    ScopedName name = (ScopedName)e.nextElement();
                    ConstrTypeSpec ts = (ConstrTypeSpec)name.resolvedTypeSpec().typeSpec();
                    if( ts.declaration() instanceof Interface )
                    {
                        continue;
                    }
                    else
                    {
                        parser.fatal_error( "Non-interface type in supported interfaces list:\n\t" +
                                            inheritanceSpec, token );
                    }
                }
            }
            NameTable.parsed_interfaces.put( full_name(), "" );
            parser.remove_pending( full_name() );
        }
        else if ( ! justAnotherOne)
        {
            // i am forward declared, must set myself as
            // pending further parsing
            parser.set_pending( full_name() );
        }

    }

    public void setEnclosingSymbol( IdlSymbol s )
    {
        if( enclosing_symbol != null && enclosing_symbol != s )
        {
            System.err.println( "was " + enclosing_symbol.getClass().getName() + " now: " + s.getClass().getName() );
            throw new RuntimeException( "Compiler Error: trying to reassign container for " + name );
        }
        enclosing_symbol = s;
        stateMembers.setEnclosingSymbol( this );
        for( Iterator i = operations.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setEnclosingSymbol( s );

        for( Iterator i = exports.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setEnclosingSymbol( s );

        for( Iterator i = factories.iterator(); i.hasNext(); )
            ( (IdlSymbol)i.next() ).setEnclosingSymbol( s );
    }

    public void set_included( boolean i )
    {
        included = i;
    }

    public boolean basic()
    {
        return true;
    }

    public String toString()
    {
        return full_name();
    }

    public String holderName()
    {
        return javaName() + "Holder";
    }

    public String typeName()
    {
        return full_name();
    }

    public String getTypeCodeExpression()
    {
        return this.getTypeCodeExpression( new HashSet() );
    }

    public String getTypeCodeExpression( Set knownTypes )
    {
        if( knownTypes.contains( this ) )
        {
            return this.getRecursiveTypeCodeExpression();
        }
        else
        {
            knownTypes.add( this );
            StringBuffer result = new StringBuffer
                    ( "org.omg.CORBA.ORB.init().create_value_tc (" +
                    // id, name
                    "\"" + id() + "\", " + "\"" + name + "\", " +
                    // type modifier
                    "(short)" +
                    ( this.isCustomMarshalled()
                    ? org.omg.CORBA.VM_CUSTOM.value
                    : org.omg.CORBA.VM_NONE.value ) + ", " +
                    // concrete base type
                    "null, " +
                    // value members
                    "new org.omg.CORBA.ValueMember[] {" );
            for( Iterator i = stateMembers.v.iterator(); i.hasNext(); )
            {
                StateMember m = (StateMember)i.next();
                result.append( getValueMemberExpression( m, knownTypes ) );
                if( i.hasNext() ) result.append( ", " );
            }
            result.append( "})" );
            return result.toString();
        }
    }

    private String getValueMemberExpression( StateMember m, Set knownTypes )
    {
        TypeSpec typeSpec = m.typeSpec();
        short access = m.isPublic
                ? org.omg.CORBA.PUBLIC_MEMBER.value
                : org.omg.CORBA.PRIVATE_MEMBER.value;
        return "new org.omg.CORBA.ValueMember (" +
                "\"" + m.name + "\", \"" + typeSpec.id() +
                "\", \"" + name + "\", \"1.0\", " +
                typeSpec.getTypeCodeExpression( knownTypes ) + ", null, " +
                "(short)" + access + ")";
    }

    public void print( PrintWriter ps )
    {
        try
        {
            String path = parser.out_dir
                    + fileSeparator
                    + pack_name.replace( '.', fileSeparator );

            File dir = new File( path );

            if( !dir.exists() )
                if( !dir.mkdirs() )
                    org.jacorb.idl.parser.fatal_error
                            ( "Unable to create " + path, null );

            printClass( dir );
            printFactory( dir );
            printHelper( dir );
            printHolder( dir );
        }
        catch( IOException e )
        {
            org.jacorb.idl.parser.fatal_error
                    ( "I/O error writing " + javaName() + ": " + e, null );
        }
    }

    public String printWriteStatement( String var_name, String streamname )
    {
        return "((org.omg.CORBA_2_3.portable.OutputStream)" + streamname + ")"
                + ".write_value (" + var_name + " );";
        //                + ".write_value (" + var_name + ", \"" + id() + "\");";
    }

    public String printReadExpression( String streamname )
    {
        return "(" + javaName() + ")"
                + "((org.omg.CORBA_2_3.portable.InputStream)" + streamname + ")"
                + ".read_value (\"" + id() + "\")";
    }

    public String printReadStatement( String var_name, String streamname )
    {
        return var_name + " = " + printReadExpression( streamname );
    }

    private void printClassComment( PrintWriter out )
    {
        out.println( "/**" );
        out.println( " *\tGenerated from IDL definition of valuetype " +
                "\"" + name + "\"" );
        out.println( " *\t@author JacORB IDL compiler " );
        out.println( " */\n" );
    }

    /**
     * Prints the abstract Java class to which this valuetype is mapped.
     */

    private void printClass( File dir ) throws IOException
    {
        File outfile = new File( dir, name + ".java" );
        PrintWriter out = new PrintWriter( new FileWriter( outfile ) );

        if( pack_name.length() > 0 )
            out.println( "package " + pack_name + ";\n" );

        printClassComment( out );

        out.println( "public abstract class " + name );


        if( inheritanceSpec != null )
        {
            boolean first = true;

            Enumeration e = inheritanceSpec.getValueTypes();
            if( e.hasMoreElements() || inheritanceSpec.truncatable != null )
            {
                out.print( "\textends " );

                if( e.hasMoreElements() )
                {
                    first = false;
                    out.print( ( (IdlSymbol)e.nextElement() ).toString() );
                }

                for( ; e.hasMoreElements(); )
                {
                    out.print( ", " + ( (IdlSymbol)e.nextElement() ).toString() );
                }

                if( inheritanceSpec.truncatable != null )
                    out.print( ( first ? "" : ", " ) + inheritanceSpec.truncatable.scopedName );

                out.println();
            }
        }

        if( this.isCustomMarshalled() )
            out.print( "\timplements org.omg.CORBA.portable.CustomValue" );
        else
            out.print( "\timplements org.omg.CORBA.portable.StreamableValue" );
        out.println();

        if( inheritanceSpec != null )
        {
            Enumeration e = inheritanceSpec.getSupportedInterfaces();
            if( e.hasMoreElements() )
            {
                for( ; e.hasMoreElements(); )
                {
                    out.print( ", " + ( (IdlSymbol)e.nextElement() ).toString() );
                }
                out.println();
            }
        }

        out.println( "{" );
        out.print( "\tprivate String[] _truncatable_ids = {\"" + id() + "\"" );
        if( inheritanceSpec != null )
        {
            String[] ids = inheritanceSpec.getTruncatableIds();
            for( int j = 0; j < ids.length; j++ )
            {
                out.print( ", \"" + ids[ j ] + "\"" );
            }
        }
        out.println( "};" );

        for( Iterator i = stateMembers.v.iterator(); i.hasNext(); )
        {
            ( (StateMember)i.next() ).print( out );
            out.println();
        }

        for( Iterator i = operations.iterator(); i.hasNext(); )
        {
            ( (Operation)i.next() ).printSignature( out, true );
            out.println();
        }

        if( !this.isCustomMarshalled() )
        {
            printWriteMethod( out );
            printReadMethod( out );
        }

        out.println( "\tpublic String[] _truncatable_ids()" );
        out.println( "\t{" );
        out.println( "\t\treturn _truncatable_ids;" );  // FIXME
        out.println( "\t}" );

        out.println( "\tpublic org.omg.CORBA.TypeCode _type()" );
        out.println( "\t{" );
        out.println( "\t\treturn " + javaName() + "Helper.type();" );
        out.println( "\t}" );

        out.println( "}" );
        out.close();
    }

    /**
     * Prints the Factory interface for this valuetype if any
     * factories were defined.
     */

    private void printFactory( File dir )
            throws IOException
    {
        if( factories.size() == 0 )
            return;

        File outfile = new File( dir, name + "ValueFactory.java" );
        PrintWriter out = new PrintWriter( new FileWriter( outfile ) );

        if( pack_name.length() > 0 )
            out.println( "package " + pack_name + ";\n" );

        printClassComment( out );

        out.println( "public interface  " + name + "ValueFactory" );
        out.println( "\textends org.omg.CORBA.portable.ValueFactory" );
        out.println( "{" );

        for( Iterator i = factories.iterator(); i.hasNext(); )
        {
            ( (InitDecl)i.next() ).print( out, name );
        }

        out.println( "}" );
        out.close();
    }


    /**
     * Prints the _write() method required by
     * org.omg.CORBA.portable.StreamableValue.
     */
    private void printWriteMethod( PrintWriter out )
    {
        out.println( "\tpublic void _write " +
                "(org.omg.CORBA.portable.OutputStream os)" );
        out.println( "\t{" );
        if( !inheritanceSpec.isEmpty() )
        {
            out.println( "\t\tsuper._write( os );" );
        }


        for( Iterator i = stateMembers.v.iterator(); i.hasNext(); )
            out.println( "\t\t" + ( (StateMember)i.next() ).writeStatement( "os" ) );
        out.println( "\t}\n" );
    }

    /**
     * Prints the _read() method required by
     * org.omg.CORBA.portable.StreamableValue.
     */
    private void printReadMethod( PrintWriter out )
    {
        out.println( "\tpublic void _read " +
                "(final org.omg.CORBA.portable.InputStream os)" );
        out.println( "\t{" );

        if( !inheritanceSpec.isEmpty() )
        {
            out.println( "\t\tsuper._read( os );" );
        }

        for( Iterator i = stateMembers.v.iterator(); i.hasNext(); )
            out.println( "\t\t" + ( (StateMember)i.next() ).readStatement( "os" ) );
        out.println( "\t}\n" );
    }

    private void printHelper( File dir ) throws IOException
    {
        File outfile = new File( dir, name + "Helper.java" );
        PrintWriter out = new PrintWriter( new FileWriter( outfile ) );

        if( pack_name.length() > 0 )
            out.println( "package " + pack_name + ";\n" );

        printClassComment( out );

        out.println( "public abstract class " + name + "Helper" );
        out.println( "{" );

        out.println( "\tprivate static org.omg.CORBA.TypeCode type = null;" );

        // insert() / extract()

        out.println( "\tpublic static void insert " +
                "(org.omg.CORBA.Any a, " + javaName() + " v)" );
        out.println( "\t{" );
        out.println( "\t\ta.insert_Value (v, v._type());" );
        out.println( "\t}" );
        out.println( "\tpublic static " + javaName() + " extract " +
                "(org.omg.CORBA.Any a)" );
        out.println( "\t{" );
        out.println( "\t\treturn (" + javaName() + ")a.extract_Value();" );
        out.println( "\t}" );

        // type() / id()

        out.println( "\tpublic static org.omg.CORBA.TypeCode type()" );
        out.println( "\t{" );
        out.println( "\t\tif (type == null)" );
        out.println( "\t\t\ttype = " + getTypeCodeExpression() + ";" );
        out.println( "\t\treturn type;" );
        out.println( "\t}" );
        out.println( "\tpublic static String id()" );
        out.println( "\t{" );
        out.println( "\t\treturn \"" + id() + "\";" );
        out.println( "\t}" );

        // read() / write()

        out.println( "\tpublic static " + javaName() + " read " +
                "(org.omg.CORBA.portable.InputStream is)" );
        out.println( "\t{" );
        out.println( "\t\treturn (" + javaName() + ")((org.omg.CORBA_2_3.portable.InputStream)is).read_value (\"" + id() + "\");" );
        out.println( "\t}" );

        out.println( "\tpublic static void write " +
                "(org.omg.CORBA.portable.OutputStream os, " +
                javaName() + " val)" );
        out.println( "\t{" );
        out.println( "((org.omg.CORBA_2_3.portable.OutputStream)os)" +
                ".write_value (val, \"" + id() + "\");" );
        out.println( "\t}" );
        out.println( "}" );
        out.close();
    }

    private void printHolder( File dir ) throws IOException
    {
        File outfile = new File( dir, name + "Holder.java" );
        PrintWriter out = new PrintWriter( new FileWriter( outfile ) );

        if( pack_name.length() > 0 )
            out.println( "package " + pack_name + ";\n" );

        printClassComment( out );

        out.println( "public final class " + name + "Holder" );
        out.println( "\timplements org.omg.CORBA.portable.Streamable" );
        out.println( "{" );
        out.println( "\tpublic " + javaName() + " value;" );
        out.println( "\tpublic " + name + "Holder () {}" );
        out.println( "\tpublic " + name + "Holder (final "
                + javaName() + " initial)" );
        out.println( "\t{" );
        out.println( "\t\tvalue = initial;" );
        out.println( "\t}" );
        out.println( "\tpublic void _read " +
                "(final org.omg.CORBA.portable.InputStream is)" );
        out.println( "\t{" );
        out.println( "\t\tvalue = " + javaName() + "Helper.read (is);" );
        out.println( "\t}" );
        out.println( "\tpublic void _write " +
                "(final org.omg.CORBA.portable.OutputStream os)" );
        out.println( "\t{" );
        out.println( "\t\t" + javaName() + "Helper.write (os, value);" );
        out.println( "\t}" );
        out.println( "\tpublic org.omg.CORBA.TypeCode _type ()" );
        out.println( "\t{" );
        out.println( "\t\treturn value._type ();" );
        out.println( "\t}" );
        out.println( "}" );
        out.close();
    }

}
