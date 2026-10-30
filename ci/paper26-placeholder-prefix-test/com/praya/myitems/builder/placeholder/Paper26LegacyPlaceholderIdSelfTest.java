package com.praya.myitems.builder.placeholder;

/** Pure-Java regression test; the real placeholder resolver remains unchanged. */
public final class Paper26LegacyPlaceholderIdSelfTest {
    private static int assertions;

    private static void check(final String prefix, final String raw, final String expected) {
        final String actual = Paper26LegacyPlaceholderId.extract(prefix, raw);
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected [" + expected + "] but got [" + actual
                    + "] for [" + raw + "]");
        }
        assertions++;
    }

    public static void main(final String[] args) {
        check("myitems", "myitems_text_ability:Venom_Blast:3:20", "text_ability:Venom_Blast:3:20");
        check("myitems", "myitems_text_myitems_attribute:damage", "text_myitems_attribute:damage");
        check("myitems", "MYITEMS_text_socket_fill:gem:2", "text_socket_fill:gem:2");
        check("myitems", "myitems_", null);
        check("myitems", "myitems", null);
        check("myitems", "other_myitems_text_power", null);
        check("myitems", "myitem_text_power", null);
        check("myitems", null, null);
        check(null, "myitems_test", null);
        check("", "_test", null);
        check("x.y", "x.y_some_thing", "some_thing");
        check("x.y", "xay_some_thing", null);
        check("myitems", "myitems_text_lorestats:Tiếng Việt", "text_lorestats:Tiếng Việt");
        System.out.println("PASS: " + assertions + " legacy MVdW identifier assertions");
    }
}
