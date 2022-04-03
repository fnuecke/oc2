package li.cil.oc2.api.inet;

public interface InternetManager {

    Task runOnInternetThreadTick(Runnable action);

    interface Task {
        void close();
    }
}
