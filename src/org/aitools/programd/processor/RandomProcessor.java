/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.processor;

import org.aitools.programd.parser.TemplateParser;
import org.aitools.programd.parser.XMLNode;
import org.aitools.programd.util.LRUCache;
import org.aitools.programd.util.MersenneTwisterFast;

/**
 * <p>
 * Handles a
 * <code><a href="http://aitools.org/aiml/TR/2001/WD-aiml/#section-random">random</a></code>
 * element.
 * </p>
 * <p>
 * This improved version (since 4.1.5) employs the notion that to achieve the
 * kind of randomness expected by users and AIML authors, the following
 * requirements exist:
 * </p>
 * <ul>
 * <li>Each <code>random</code> element should have its own
 * &quot;space&quot;. This means that, for example, if <code>random</code>
 * element <code>A</code> contains five <code>li</code> children, and
 * <code>random</code> element <code>B</code> contains seven <code>li</code>
 * children, the probability that any given <code>li</code> of <code>A</code>
 * will be chosen when <code>A</code> is activated should consistently be 1:5,
 * and the probability that any given <code>li</code> of <code>B</code> will
 * be chosen when <code>B</code> is chosen should consistently be 1:7.
 * Essentially, each <code>random</code> must have its own unique series of
 * random numbers.</li>
 * <li>A &quot;unique space&quot; requirement exists as well on a per-user
 * basis: each user should have an equivalent experience of randomness for each
 * <code>random</code> element, independent of any other users.</li>
 * <li>The individual bot also has a uniqueness requirement, multiplying the
 * previous two. In effect, if there are <code>m</code> bots, <code>n</code>
 * users, and <code>p</code> random elements, there are (potentially)
 * <code>m * n * n</code> independent random number series.</li>
 * </ul>
 * 
 * @version 4.1.5
 * @author Noel Bush
 * @author Jon Baer
 * @author Thomas Ringate, Pedro Colla
 * @author Jay Myers
 */
public class RandomProcessor extends AIMLProcessor
{
    public static final String label = "random";

    /** The string &quot;li&quot;, for convenience. */
    private static final String LI = "li";

    /**
     * The map in which MersenneTwisterFast random number generators will be
     * stored for each unique botid + userid + random element.
     */
    private static final LRUCache<String, MersenneTwisterFast> generators = new LRUCache<String, MersenneTwisterFast>(100);

    public String process(int level, XMLNode tag, TemplateParser parser) throws AIMLProcessorException
    {
        if (tag.XMLType == XMLNode.TAG)
        {
            // Empty <random></random> doesn't produce anything.
            if (tag.XMLChild == null)
            {
                return EMPTY_STRING;
            } 

            // Construct the identifying string (botid + userid + element
            // contents).
            String identifier = parser.getBotID() + parser.getUserID() + tag.toString();

            // Does the generators map already contain this one?
            MersenneTwisterFast generator = generators.get(identifier);
            if (generator == null)
            {
                generator = new MersenneTwisterFast(System.currentTimeMillis());
                generators.put(identifier, generator);
            } 

            int nodeCount = parser.nodeCount(LI, tag.XMLChild, false);

            // Zero <li></li> children means that there's nothing to do.
            if (nodeCount == 0)
            {
                return EMPTY_STRING;
            } 

            // Only one <li></li> child means we don't have to pick anything.
            if (nodeCount == 1)
            {
                return parser.evaluate(level++, parser.getNode(LI, tag.XMLChild, 1).XMLChild);
            } 

            // Select a random element of the listitem.
            return parser
                    .evaluate(level++, parser.getNode(LI, tag.XMLChild, generator.nextInt(nodeCount) + 1).XMLChild);
        } 
        // (otherwise...)
        throw new AIMLProcessorException("<random></random> must have content!");
    } 
}