public class Test {
    public static void main(String[] args) {
        String nextMe = "Text from" + nextMethod();
        String firstText = "Java Platform Debugger Architecture";
        System.out.println("Hi Everyone, Welcome to " + firstText); //add a break point here
        String secondText = "Java Debug Interface"; //add a break point here and also stepping in here
        String text = "Tbdaklsa" + secondText;
        System.out.println(text + nextMe);
    }

    public static String nextMethod() {
        return "another method";
    }

}
