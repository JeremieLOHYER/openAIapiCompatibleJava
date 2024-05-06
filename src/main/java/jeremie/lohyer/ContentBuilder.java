package jeremie.lohyer;

import java.util.Arrays;
import java.util.function.Function;

public class ContentBuilder {

    public abstract static class Content {
        String name;

        public Content(String name) {
            this.name = name;
        }
    }

    public static class ContentClass {
        Content[] content;

        public ContentClass(Content[] content) {
            this.content = content;
        }

        public ContentClass(Content content) {
            this.content = new Content[]{content};
        }
    }

    public static class ContentArray extends Content {
        ContentClass[] content;

        public ContentArray(String name, ContentClass[] content) {
            super(name);
            this.content = content;
        }

        public ContentArray(String name, Content[] content) {
            super(name);
            this.content = Arrays.stream(content).map(ContentClass::new).toArray(ContentClass[]::new);
        }
    }

    public static class ContentText extends Content {
        String content;

        public ContentText(String name, String content) {
            super(name);
            this.content = content;
        }
    }

    private String content;

    ContentBuilder() {
        content = "";
    }

    ContentBuilder(Content content) {
        this.content = "";
        addContent(content, 0);
    }

    private void addHeight(int height) {
        for (int i = 0; i < height; i++) {
            content += "\t";
        }
    }

    public ContentBuilder addContent(Content content, int height) {
        if (content.getClass().equals(ContentArray.class)) {
            return addContent((ContentArray) content, height + 1);
        } else if (content.getClass().equals(ContentText.class)) {
            return addContent((ContentText) content, height + 1);
        }
        return this;
    }

    public ContentBuilder addContent(ContentClass c, int height) {
        //addHeight(height);
        this.content += "{";
        for (int i = 0; i < c.content.length - 1; i++) {
            addContent(c.content[i], height);
            this.content += ",";
        }
        addContent(c.content[c.content.length - 1], height);
        //this.content += "\n";
        //addHeight(height);
        this.content += "}";
        return this;
    }

    public ContentBuilder addContent(ContentText c, int height) {
        //addHeight(height);
        this.content += "\"" + c.name + "\": \"" + c.content + "\"" ;
        return this;
    }


    public ContentBuilder addContent(ContentArray c, int height) {
        //addHeight(height);
        this.content += "\"" + c.name + "\": [";
        for (int i = 0; i < c.content.length - 1; i++) {
            addContent(c.content[i], height + 1);
            this.content += ",";
        }
        addContent(c.content[c.content.length - 1], height + 1);
        //this.content += "\n";
        //addHeight(height);
        this.content += "]";
        return this;
    }

    public String build() {
        return content;
    }
}
