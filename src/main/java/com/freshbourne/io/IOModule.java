/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 *
 * http://creativecommons.org/licenses/by-nc/3.0/
 *
 * For alternative conditions contact the author.
 *
 * Copyright (c) 2011 "Robin Wenglewski <robin@wenglewski.de>"
 */

package com.freshbourne.io;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class IOModule extends AbstractModule{
    private static final Logger LOG = Logger.getLogger(IOModule.class);
	
	// ***** CONFIGURATION (CONSTRUCTURS) *****
	public File file;
	public int pageSize = PageSize.DEFAULT_PAGE_SIZE;
	public int cacheSize = 10;
    public boolean doLock = true;

    public void resourceFile(final File file){this.file = file;};
    public void pageSize(final int i){pageSize = i;}

	
	// ***** CONFIGURE *****
	
	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
        if(pageSize != -1)
		    bind(Integer.class).annotatedWith(PageSize.class).toInstance(pageSize);

        if(file != null)
		    bind(File.class).annotatedWith(ResourceFile.class).toInstance(file);

        bind(Boolean.class).annotatedWith(Names.named("doLock")).toInstance(doLock);

		bindConstant().annotatedWith(Names.named("cacheSize")).to(cacheSize);
		
		bind(new TypeLiteral<PageManager<RawPage>>(){}).to(ResourceManager.class).in(Singleton.class);
	}

    @Provides @Singleton
	public ResourceManager provideFileResourceManager(@PageSize final int pageSize) {
        LOG.info("getPageSize: " + pageSize);
		final ResourceManager result = new ResourceManagerBuilder().file(file).useCache(false).build();
		try {
			result.open();
		} catch (IOException e) {
			e.printStackTrace();
			// throw no runtime error, since the user can handle the fact that the resource is closed
		}
		return result;
	}

    public void setFile(final File file) {
        this.file = file;
    }
}
