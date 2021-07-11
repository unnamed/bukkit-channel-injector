package me.fixeddev.channelinjector.list;

public interface Listener<E> {
    default void onRemove(Object element) {
    }

    default void onAdd(E element) {
    }
}
