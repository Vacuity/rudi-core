package ai.vacuity.rudi.adaptors.interfaces.impl;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;
import ai.vacuity.rudi.adaptors.interfaces.INamespaceProvider;

public class DefaultNamespaceProvider implements INamespaceProvider {

	@Override
	public Namespace getNamespace(String name) {
		String prefix = GraphManager.getRepository().getConnection().getNamespace(name);
		return (StringUtils.isNotEmpty(prefix)) ? new SimpleNamespace(prefix, name) : null;
	}

}
