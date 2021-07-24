package ch.ethz.lapis.core;

public abstract class SubProgram<T extends Config> {

    private final String name;
    private final Class<T> configClass;


    public SubProgram(String name, Class<T> configClass) {
        this.name = name;
        this.configClass = configClass;
    }


    public String getName() {
        return name;
    }


    public Class<T> getConfigClass() {
        return configClass;
    }


    public abstract void run(String[] args, T config) throws Exception;
}
