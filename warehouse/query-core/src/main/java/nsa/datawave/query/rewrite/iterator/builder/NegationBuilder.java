package nsa.datawave.query.rewrite.iterator.builder;

import nsa.datawave.query.rewrite.iterator.NestedIterator;

/**
 * Used this to build negation subtrees.
 * 
 */
public class NegationBuilder extends AbstractIteratorBuilder {
    
    @Override
    public <T> NestedIterator<T> build() {
        throw new UnsupportedOperationException();
    }
    
}