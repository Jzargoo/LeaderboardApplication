package com.jzargo.core.mapper;

public interface Mapper <F,T>{
    T map(F from);

    default T map(F from, T old) {
        return old;
    }
}
