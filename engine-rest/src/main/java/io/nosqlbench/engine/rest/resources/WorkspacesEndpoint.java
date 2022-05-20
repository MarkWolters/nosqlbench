package io.nosqlbench.engine.rest.resources;

/*
 * Copyright (c) 2022 nosqlbench
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import io.nosqlbench.docsys.api.WebServiceObject;
import io.nosqlbench.engine.rest.services.WorkSpace;
import io.nosqlbench.engine.rest.services.WorkspaceFinder;
import io.nosqlbench.engine.rest.transfertypes.WorkspaceItemView;
import io.nosqlbench.engine.rest.transfertypes.WorkspaceView;
import io.nosqlbench.nb.annotations.Service;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service(value = WebServiceObject.class, selector = "workspaces")
@Path("/services/workspaces")
@Singleton
public class WorkspacesEndpoint implements WebServiceObject {

    private final static Logger logger = LogManager.getLogger(WorkspacesEndpoint.class);

    @Context
    private Configuration config;

    private final static java.nio.file.Path workspacesRoot = Paths.get("workspaces");
    private WorkspaceFinder svc;

    /**
     * @return A list of workspaces as a
     * {@link List} of {@link WorkspaceView}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkspacesInfo() {
        List<WorkspaceView> wsviews = getSvc().getWorkspaceViews();
        return Response.ok(wsviews).build();
    }

    @DELETE
    @Path("/{workspace}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteWorkspace(@PathParam("workspace") String workspace,
                                    @QueryParam("deleteCount") String deleteCount) {
        try {
            int dc = deleteCount != null ? Integer.valueOf(deleteCount) : 0;
            getSvc().purgeWorkspace(workspace, dc);
            return Response.ok("removed workspace " + workspace).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{workspaceName}/{filepath:.+}")
    public Response listFilesInWorkspace(
        @PathParam("workspaceName") String workspaceName,
        @PathParam("filepath") String filepath
    ) {
        try {
            WorkSpace w = getSvc().getWorkspace(workspaceName);
            List<WorkspaceItemView> listing = w.getWorkspaceListingView(filepath);
            return Response.ok(listing).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }


    @POST
    @Path("/{workspaceName}/upload/{filepath}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFileIntoWorkspace(
    ) {
        return Response.ok().build();
    }

    @PUT
    @Path("/{workspaceName}/{filepath:.+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response doSomething(@Context HttpServletRequest request, byte[] input) {
        logger.debug("Content-Type: {}", request.getContentType());
        logger.debug("Preferred output: {}", request.getHeader(HttpHeaders.ACCEPT));
        try {
            String pathInfo = request.getPathInfo();
            String[] parts = pathInfo.split("/");
            String workspaceName = parts[parts.length - 2];
            String filename = parts[parts.length - 1];
            getSvc().putFile(workspaceName, filename, ByteBuffer.wrap(input));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("/{workspace}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkspaceInfo(
        @PathParam("workspace") String workspace,
        @QueryParam("ls") String ls,
        @QueryParam("contains") String contains
    ) {
        try {
            WorkSpace ws = getSvc().getWorkspace(workspace);
            WorkspaceView wsview = ws.getWorkspaceView();
            if (ls != null && !ls.equalsIgnoreCase("false")) {
                List<WorkspaceItemView> listing = ws.getWorkspaceListingView("");
                if (contains != null) {
                    listing = listing.stream().filter(i -> i.contains(contains)).collect(Collectors.toList());
                }
                wsview.setListing(listing);
            }
            return Response.ok(wsview).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("/{workspace}/{filename}")
    public Response getFileInWorkspace(
        @PathParam("workspace") String workspace,
        @PathParam("filename") String filename,
        @QueryParam("ls") String ls) {

        try {
            if (ls != null && !ls.equalsIgnoreCase("false")) {
                WorkSpace ws = getSvc().getWorkspace(workspace);
                List<WorkspaceItemView> listing = ws.getWorkspaceListingView(filename);
                return Response.ok(listing).build();

            } else {
                WorkspaceFinder.FileInfo fileinfo = getSvc().readFile(workspace, filename);
                if (fileinfo != null) {
                    return Response.ok(fileinfo.getPath().toFile(), fileinfo.getMediaType()).build();
                } else {
                    return Response.noContent().status(Response.Status.NOT_FOUND).build();
                }
            }
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    private WorkspaceFinder getSvc() {
        if (svc == null) {
            svc = new WorkspaceFinder(config);
        }
        return svc;
    }

}
