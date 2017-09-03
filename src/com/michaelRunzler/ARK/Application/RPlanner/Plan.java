import java.io.Serializable;

public class Plan implements Serializable
{
    private String[] components;
    private String name;
    private String classIdentifier;

    public Plan(String name, String classIdentifier, String... components)
    {
        this.name = name;
        this.classIdentifier = classIdentifier;
        this.components = components;
    }

    public String getClassIdentifier() {
        return classIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getComponents() {
        return components;
    }

    public void setComponents(String[] components) {
        this.components = components;
    }

    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        temp.append(this.name);
        boolean flag = true;
        for(String s : components){
            temp.append(flag ? ": " : ", ");
            temp.append(s);
            flag = false;
        }
        return temp.toString();
    }
}
