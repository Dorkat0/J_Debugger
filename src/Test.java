import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        String firstText = "Java Platform Debugger Architecture";
        String nextMe = "Text from" + nextMethod();
        System.out.println("Hi Everyone, Welcome to " + firstText); //add a break point here
        String secondText = "Java Debug Interface"; //add a break point here and also stepping in here
        String text = "Tbdaklsa" + secondText;
        List<String> moreText= new ArrayList<>();
        moreText.add("TEst1");
        moreText.add("Test2");
        System.out.println(text + nextMe);
    }

    public static String nextMethod() {
        String newValueInThisMethod =  "another method";
        return newValueInThisMethod;
    }

}
