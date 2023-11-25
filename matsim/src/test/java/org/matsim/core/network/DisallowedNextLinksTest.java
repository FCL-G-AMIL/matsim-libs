package org.matsim.core.network;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.utils.objectattributes.attributeconverters.DisallowedNextLinksAttributeConverter;

public class DisallowedNextLinksTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testEquals() {
		DisallowedNextLinks dnl0 = new DisallowedNextLinks();
		DisallowedNextLinks dnl1 = new DisallowedNextLinks();
		dnl0.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));
		dnl1.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));

		Assert.assertEquals(dnl0, dnl1);
		Assert.assertEquals(dnl1, dnl0);

		dnl1.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("0")));

		Assert.assertNotEquals(dnl0, dnl1);
		Assert.assertNotEquals(dnl1, dnl0);
	}

	@Test
	public void testIsEmpty() {
		DisallowedNextLinks dnl= new DisallowedNextLinks();
		Assert.assertTrue(dnl.isEmpty());
	}

	@Test
	public void testAdding() {
		DisallowedNextLinks dnl = new DisallowedNextLinks();
		dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));
		dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0")));
		dnl.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("0")));

		Map<String, List<List<Id<Link>>>> map = Map.of(
				"bike", List.of(List.of(Id.createLinkId("0"))),
				"car", List.of(List.of(Id.createLinkId("0"), Id.createLinkId("1")), List.of(Id.createLinkId("0"))));
		Assert.assertEquals(map, dnl.getAsMap());
	}

	@Test
	public void testRemoving() {
		DisallowedNextLinks dnl = new DisallowedNextLinks();
		dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));
		dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0")));
		dnl.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("0")));

		dnl.removeDisallowedLinkSequences("bike");

		Map<String, List<List<Id<Link>>>> map = Map.of(
				"car", List.of(List.of(Id.createLinkId("0"), Id.createLinkId("1")), List.of(Id.createLinkId("0"))));
		Assert.assertEquals(map, dnl.getAsMap());
	}

	@Test
	public void testNotAddingDuplicates() {
		DisallowedNextLinks dnl = new DisallowedNextLinks();

		Assert.assertTrue(dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1"))));
		Assert.assertFalse(dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1"))));
		Assert.assertFalse(dnl.addDisallowedLinkSequence("car", Collections.emptyList()));
	}

	@Test
	public void testNotAddingEmpty() {
		DisallowedNextLinks dnl = new DisallowedNextLinks();

		Assert.assertFalse(dnl.addDisallowedLinkSequence("car", Collections.emptyList()));
	}

	@Test
	public void testNotAddingSequenceWithDuplicates() {
		DisallowedNextLinks dnl = new DisallowedNextLinks();

		Assert.assertFalse(dnl.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("0"))));
	}

	@Test
	public void testEqualAndHashCode() {
		DisallowedNextLinks dnl0 = new DisallowedNextLinks();
		dnl0.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));
		dnl0.addDisallowedLinkSequence("car", List.of(Id.createLinkId("4"), Id.createLinkId("5")));
		DisallowedNextLinks dnl1 = new DisallowedNextLinks();
		dnl1.addDisallowedLinkSequence("car", List.of(Id.createLinkId("4"), Id.createLinkId("5")));
		dnl1.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("1")));

		Assert.assertEquals(dnl0, dnl1);
		Assert.assertEquals(dnl0.hashCode(), dnl1.hashCode());
	}

	@Test
	public void testSerialization() {
		DisallowedNextLinks dnl0 = new DisallowedNextLinks();
		dnl0.addDisallowedLinkSequence("car", List.of(Id.createLinkId("0"), Id.createLinkId("3")));
		dnl0.addDisallowedLinkSequence("car",
				List.of(Id.createLinkId("0"), Id.createLinkId("1"), Id.createLinkId("2")));
		dnl0.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("10"), Id.createLinkId("11")));
		DisallowedNextLinksAttributeConverter ac = new DisallowedNextLinksAttributeConverter();
		String s = ac.convertToString(dnl0);
		DisallowedNextLinks dnl1 = ac.convert(s);

		Assert.assertEquals("{\"car\":[[\"0\",\"1\",\"2\"],[\"0\",\"3\"]],\"bike\":[[\"10\",\"11\"]]}", s);
		Assert.assertEquals(dnl0, dnl1);
		Assert.assertEquals(dnl0.hashCode(), dnl1.hashCode());
		Assert.assertSame(dnl0.getDisallowedLinkSequences("car").get(0).get(0),
				dnl1.getDisallowedLinkSequences("car").get(0).get(0));
	}

	@Test
	public void testNetworkWritingAndReading() throws IOException {

		Network n = createNetwork();
		Link l1 = n.getLinks().get(Id.createLinkId("1"));
		DisallowedNextLinks dnl0 = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl0.addDisallowedLinkSequence("car", List.of(l1.getId(), Id.createLinkId("2")));

		File tempFile = tempFolder.newFile("network.xml");
		new NetworkWriter(n).write(tempFile.toString());
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(tempFile.toString());

		DisallowedNextLinks dnl1 = NetworkUtils.getDisallowedNextLinks(network.getLinks().get(l1.getId()));
		Assert.assertEquals(dnl0, dnl1);
		Assert.assertEquals(dnl0.hashCode(), dnl1.hashCode());
		Assert.assertSame(l1.getId(), dnl1.getDisallowedLinkSequences("car").get(0).get(0));
	}

	static Network createNetwork() {
		// Nodes
		//             n3
		// n0 --- n1 <    > n4 --- n5
		//             n2
		// Links
		//              l2 - * - l5
		// * - l0 - * <             > * - l4 - *
		//              l1 - * - l3
		Network n = NetworkUtils.createNetwork();
		Node n0 = NetworkUtils.createAndAddNode(n, Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createAndAddNode(n, Id.createNodeId("1"), new Coord(1, 0));
		Node n2 = NetworkUtils.createAndAddNode(n, Id.createNodeId("2"), new Coord(2, -1));
		Node n3 = NetworkUtils.createAndAddNode(n, Id.createNodeId("3"), new Coord(2, 1));
		Node n4 = NetworkUtils.createAndAddNode(n, Id.createNodeId("4"), new Coord(3, 0));
		Node n5 = NetworkUtils.createAndAddNode(n, Id.createNodeId("5"), new Coord(4, 0));
		NetworkUtils.createAndAddLink(n, Id.createLinkId("0"), n0, n1, Math.sqrt(2), 30 / 3.6, 900, 1);
		NetworkUtils.createAndAddLink(n, Id.createLinkId("1"), n1, n2, Math.sqrt(2), 30 / 3.6, 900, 1);
		NetworkUtils.createAndAddLink(n, Id.createLinkId("2"), n1, n3, Math.sqrt(2), 30 / 3.6, 900, 1);
		NetworkUtils.createAndAddLink(n, Id.createLinkId("3"), n2, n4, Math.sqrt(2), 30 / 3.6, 900, 1);
		NetworkUtils.createAndAddLink(n, Id.createLinkId("4"), n3, n4, Math.sqrt(2), 30 / 3.6, 900, 1);
		NetworkUtils.createAndAddLink(n, Id.createLinkId("5"), n4, n5, Math.sqrt(2), 30 / 3.6, 900, 1);
		return n;
	}

}
