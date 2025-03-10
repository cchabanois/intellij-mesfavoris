package mesfavoris.internal.toolwindow;

import com.intellij.util.ui.tree.AbstractTreeModel;

import java.io.File;

public class MesFavorisTreeModel extends AbstractTreeModel {


    String root;

    public MesFavorisTreeModel() {
        this( System.getProperty( "user.home" ));
    }

    public MesFavorisTreeModel( String startPath ) {
        root = startPath;
    }

    public Object getRoot() {
        return new File( root );
    }

    public Object getChild( Object parent, int index ) {
        File directory = (File)parent;
        String[] children = directory.list();
        return new File( directory, children[index] );
    }

    public int getChildCount( Object parent ) {
        File fileSysEntity = (File)parent;
        if ( fileSysEntity.isDirectory() ) {
            String[] children = fileSysEntity.list();
            return children.length;
        }
        else {
            return 0;
        }
    }

    public boolean isLeaf( Object node ) {
        return ((File)node).isFile();
    }

    public int getIndexOfChild( Object parent, Object child ) {
        File directory = (File)parent;
        File fileSysEntity = (File)child;
        String[] children = directory.list();
        int result = -1;

        for ( int i = 0; i < children.length; ++i ) {
            if ( fileSysEntity.getName().equals( children[i] ) ) {
                result = i;
                break;
            }
        }

        return result;
    }
}
