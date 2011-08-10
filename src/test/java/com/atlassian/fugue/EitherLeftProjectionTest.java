package com.atlassian.fugue;

import static com.atlassian.fugue.Either.left;
import static com.atlassian.fugue.Either.right;
import static com.atlassian.fugue.UtilityFunctions.reverse;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

public class EitherLeftProjectionTest
{
    private final Either<String, Integer> l = left("heyaa!");
    private final Either<String, Integer> r = right(12);
    final Supplier<String> boo = new Supplier<String>()
    {
        @Override
        public String get()
        {
            return "boo!";
        }
    };
    Function<String, Either<String, Integer>> reverseToEither = new Function<String, Either<String, Integer>>()
    {
        @Override
        public Either<String, Integer> apply(final String from)
        {
            return left(reverse.apply(from));
        }
    };

    @Test
    public void isDefined()
    {
        assertThat(l.left().isDefined(), is(true));
    }

    @Test
    public void isNotDefined()
    {
        assertThat(r.left().isDefined(), is(false));
    }

    @Test
    public void isEmpty()
    {
        assertThat(r.left().isEmpty(), is(true));
    }

    @Test
    public void isNotEmpty()
    {
        assertThat(l.left().isEmpty(), is(false));
    }

    @Test
    public void either()
    {
        assertThat(l.left().either(), is(l));
        assertThat(r.left().either(), is(r));
    }

    @Test
    public void iteratorNotEmpty()
    {
        assertThat(l.left().iterator().next(), is("heyaa!"));
    }

    @Test
    public void iteratorEmpty()
    {
        assertThat(r.left().iterator().hasNext(), is(false));
    }

    @Test
    public void getOrNullDefined()
    {
        assertThat(l.left().getOrNull(), is("heyaa!"));
    }

    @Test
    public void getOrNullEmpty()
    {
        assertThat(r.left().getOrNull(), nullValue());
    }

    @Test
    public void getOrErrorDefined()
    {
        assertThat(l.left().getOrError(boo), is("heyaa!"));
    }

    @Test(expected = AssertionError.class)
    public void getOrErrorEmpty()
    {
        r.left().getOrError(boo);
    }

    @Test
    public void getOrElseDefined()
    {
        assertThat(l.left().getOrElse("foo"), is("heyaa!"));
    }

    @Test
    public void getOrElseEmpty()
    {
        assertThat(r.left().getOrElse("foo"), is("foo"));
    }

    @Test
    public void getOrElseSupplierDefined()
    {
        assertThat(l.left().getOrElse(boo), is("heyaa!"));
    }

    @Test
    public void getOrElseSupplierEmpty()
    {
        assertThat(r.left().getOrElse(boo), is("boo!"));
    }

    @Test
    public void existsDefinedTrue()
    {
        assertThat(l.left().exists(Predicates.<String> alwaysTrue()), is(true));
    }

    @Test
    public void existsDefinedFalse()
    {
        assertThat(l.left().exists(Predicates.<String> alwaysFalse()), is(false));
    }

    @Test
    public void existsNotDefinedTrue()
    {
        assertThat(r.left().exists(Predicates.<String> alwaysTrue()), is(false));
    }

    @Test
    public void existsNotDefinedFalse()
    {
        assertThat(r.left().exists(Predicates.<String> alwaysFalse()), is(false));
    }

    @Test
    public void forallDefinedTrue()
    {
        assertThat(l.left().forall(Predicates.<String> alwaysTrue()), is(true));
    }

    @Test
    public void forallDefinedFalse()
    {
        assertThat(l.left().forall(Predicates.<String> alwaysFalse()), is(false));
    }

    @Test
    public void forallNotDefinedTrue()
    {
        assertThat(r.left().forall(Predicates.<String> alwaysTrue()), is(true));
    }

    @Test
    public void forallNotDefinedFalse()
    {
        assertThat(r.left().forall(Predicates.<String> alwaysFalse()), is(true));
    }

    @Test
    public void foreachDefined()
    {
        assertThat(Count.countEach(l.left()), is(1));
    }

    @Test
    public void foreachNotDefined()
    {
        assertThat(Count.countEach(r.left()), is(0));
    }

    @Test
    public void onDefined()
    {
        assertThat(l.left().on(Functions.toStringFunction()), is("heyaa!"));
    }

    @Test
    public void onNotDefined()
    {
        assertThat(r.left().on(Functions.toStringFunction()), is("12"));
    }

    @Test
    public void mapDefined()
    {
        assertThat(l.left().map(reverse).left().get(), is("!aayeh"));
    }

    @Test
    public void mapNotDefined()
    {
        assertThat(r.left().map(reverse).right().get(), is(12));
    }

    @Test
    public void flatMapDefined()
    {
        assertThat(l.left().flatMap(reverseToEither).left().get(), is("!aayeh"));
    }

    @Test
    public void flatMapNotDefined()
    {
        assertThat(r.left().flatMap(reverseToEither).right().get(), is(12));
    }

    @Test
    public void sequenceDefined()
    {
        final Either<String, Integer> e = left("bar");
        assertThat(l.left().sequence(e).left().get(), is("bar"));
    }

    @Test
    public void sequenceNotDefined()
    {
        final Either<String, Integer> e = left("bar");
        assertThat(r.left().sequence(e).right().get(), is(12));
    }

    @Test
    public void filterDefinedTrue()
    {
        final Option<Either<String, Object>> filtered = l.left().filter(Predicates.<String> alwaysTrue());
        assertThat(filtered.isDefined(), is(true));
        assertThat(filtered.get().left().isDefined(), is(true));
    }

    @Test
    public void filterDefinedFalse()
    {
        final Option<Either<String, Object>> filtered = l.left().filter(Predicates.<String> alwaysFalse());
        assertThat(filtered.isDefined(), is(false));
    }

    @Test
    public void filterNotDefined()
    {
        final Option<Either<String, Object>> filtered = r.left().filter(Predicates.<String> alwaysTrue());
        assertThat(filtered.isDefined(), is(false));
    }

    @Test
    public void applyDefinedLeft()
    {
        final Either<Function<String, String>, Integer> func = left(reverse);
        assertThat(l.left().apply(func).left().get(), is("!aayeh"));
    }

    @Test
    public void applyDefinedRight()
    {
        final Either<Function<String, String>, Integer> func = right(36);
        assertThat(l.left().apply(func).right().get(), is(36));
    }

    @Test
    public void applyNotDefinedLeft()
    {
        final Either<Function<String, String>, Integer> func = left(reverse);
        assertThat(r.left().apply(func).right().get(), is(12));
    }

    @Test
    public void applyNotDefinedRight()
    {
        final Either<Function<String, String>, Integer> func = right(36);
        assertThat(r.left().apply(func).right().get(), is(36));
    }
}