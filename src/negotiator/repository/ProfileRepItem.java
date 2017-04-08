package negotiator.repository;

import java.net.URL;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import negotiator.Domain;
import negotiator.exceptions.Warning;
import negotiator.session.RepositoryException;
import negotiator.utility.UtilitySpace;

@XmlRootElement
public class ProfileRepItem implements RepItem {
	private static final long serialVersionUID = -5071749178482314158L;
	@XmlAttribute
	private URL url;
	@XmlTransient
	private DomainRepItem domain;

	private ProfileRepItem() {
		try {
			this.url = new URL("file:xml-failed-to-set-url");
		} catch (Exception e) {
			new Warning("failed to set filename default value" + e);
		}
	}

	public ProfileRepItem(URL file, DomainRepItem dom) {
		this.url = file;
		this.domain = dom;
	}

	public URL getURL() {
		return this.url;
	}

	public DomainRepItem getDomain() {
		return this.domain;
	}

	public String toString() {
		return getURL().getFile();
	}

	public String getFullName() {
		return this.url.toString().replaceAll("/", ".").replace(":", ".");
	}

	public void afterUnmarshal(Unmarshaller u, Object parent) {
		if ((parent instanceof DomainRepItem)) {
			this.domain = ((DomainRepItem) parent);
		} else {
			this.domain = searchDomain();
		}
	}

	private DomainRepItem searchDomain() {
		try {
			for (RepItem item : RepositoryFactory.get_domain_repos().getItems()) {
				DomainRepItem repitem = (DomainRepItem) item;
				if (repitem.getProfiles().contains(this)) {
					return repitem;
				}
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		System.err.println("The profile " + this + " is not in the domain repository, failed to unmarshall");
		throw new NullPointerException("no domain found");
	}

	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.url != null ? this.url.hashCode() : 0);
		return hash;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ProfileRepItem)) {
			return false;
		}
		return this.url.equals(((ProfileRepItem) o).getURL());
	}

	public String getName() {
		String name = this.url.getFile();
		if ((name.contains("/")) && (name.contains("."))) {
			name = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
		}
		return name;
	}

	public UtilitySpace create() throws RepositoryException {
		try {
			Domain domain = RepositoryFactory.get_domain_repos().getDomain(getDomain());

			return RepositoryFactory.get_domain_repos().getUtilitySpace(domain, this);
		} catch (Exception e) {
			throw new RepositoryException("File not found for " + this, e);
		}
	}
}
