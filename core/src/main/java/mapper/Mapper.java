package mapper;

public interface Mapper<F,T>{
    T map(F from);
    default T map(F from, T to) {
        return
                to;
    }
}
