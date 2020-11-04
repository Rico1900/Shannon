package edu.nju.seg.expression;

import edu.nju.seg.model.Fragment;
import edu.nju.seg.model.LoopFragment;
import edu.nju.seg.model.SDComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FragmentTest {

    @Test
    public void test_fragment_cast()
    {
        SDComponent c = new LoopFragment(new ArrayList<>(), new ArrayList<>(), "loop", 3, 3);
        Fragment f = (Fragment) c;
        assertTrue(f instanceof LoopFragment);
    }

}
