import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        String firstText = "Java Platform Debugger Architecture";
        String nextMe = "Text from" + nextMethod();
        System.out.println("Hi Everyone, Welcome to " + firstText); //add a break point here
        String secondText = "Java Debug Interface"; //add a break point here and also stepping in here
        String text = "Tbdaklsa" + secondText;
        String[] moreText= new String[]{"Ani","Sam","Joe"};    //inline initialization  
        System.out.println(moreText);
        System.out.println(text + nextMe);
    }

    public static String nextMethod() {
        String newValueInThisMethod =  "another method";
        return newValueInThisMethod;
    }

}
