use async-fix

db.zendeskMapping.find({zendeskId: 371430463600})

// user aggregation
db.zendeskUser.aggregate([
    // {$match: {operationId: "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"}},
    // {$match: {operationId: "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d", _id: 372687935519}},
    {$match: {_id: {$in: [371430366420, 372687935519, 362415139339]}}},
    {$lookup: {from: "zendeskMapping", localField: "organizationId", foreignField: "zendeskId", as: "organization"}},
    {$lookup: {from: "zendeskGroupMembership", localField: "_id", foreignField: "userId", as: "groupMembership"}},
    {$unwind: {path: "$groupMembership", preserveNullAndEmptyArrays: true}},
    {$lookup: {from: "zendeskMapping", localField: "groupMembership.groupId", foreignField: "zendeskId", as: "grispiGroupIds"}},
    {$unwind: {path: "$grispiGroupIds", preserveNullAndEmptyArrays: true}},
    {$group: {_id: "$_id", user: {$first: "$$ROOT"}, grispiOrganizationId: {$first: "$organization.grispiId"}, grispiGroupIds: {$push: "$grispiGroupIds.grispiId"}}},
    {$project: {user:1, grispiOrganizationId: {$arrayElemAt: ["$grispiOrganizationId", 0]}, grispiGroupIds: 1}}
])

db.runCommand({
    aggregate: 'zendeskUser',
    pipeline: [
        {$match: {operationId: '45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d', defaultGroupId: {$ne: null}}},
        {$limit: 2}
    ],
    cursor: {}
})

// ticket aggregation

db.zendeskTicket.aggregate([
    {$match: {_id: 94490}},
    {$lookup: {from: "zendeskMapping", as: "assignee", localField: "assigneeId", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "followers", localField: "followerIds", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "emailCcs", localField: "emailCcIds", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "submitter", localField: "submitterId", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "requester", localField: "requesterId", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "group", localField: "groupId", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "ticketForm", localField: "ticketFormId", foreignField: "zendeskId"}},
    {$project: {
            _id: 1,
            ticket: '$$ROOT',
            grispiMappings: {
                assigneeId: {$arrayElemAt: ['$assignee.grispiId', 0]},
                followerIds: "$followers.grispiId",
                emailCcIds: "$emailCcs.grispiId",
                submitterId: {$arrayElemAt: ["$submitter.grispiId", 0]},
                requesterId: {$arrayElemAt: ["$requester.grispiId", 0]},
                groupId: {$arrayElemAt: ["$group.grispiId", 0]},
                ticketFormId: {$arrayElemAt: ["$ticketForm.grispiId", 0]},
            }
        }}
])

db.zendeskTicket.aggregate([
    {$match: {followerIds: {$ne: []}}},
    // {$limit: 10},
    // assignee id
    {$lookup: {from: "zendeskMapping", as: "assignee", localField: "assigneeId", foreignField: "zendeskId"}},
    {$project: {
            _id: 1,
            ticket: '$$ROOT',
            grispiMappings: {assigneeId: {$arrayElemAt: ['$assignee.grispiId', 0]}}
        }}
])


// comment aggregation
db.zendeskComment.aggregate([
    {$match: {operationId: "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"}},
    {$lookup: {from: "zendeskMapping", as: "ticketKeyMapping", localField: "ticketId", foreignField: "zendeskId"}},
    {$lookup: {from: "zendeskMapping", as: "authorIdMapping", localField: "authorId", foreignField: "zendeskId"}},
    {$unwind: "$ticketKeyMapping"},
    {$unwind: "$authorIdMapping"},
    {$project: {body: 1, ticketKey: "$ticketKeyMapping.grispiId", createdAt: 1, publicVisible: "$public", attachments: 1, creator: {id: "$authorIdMapping.grispiId"}}},
    {$group:{ _id: "$ticketKey", comments: { $push: "$$ROOT" }}}
], {
    allowDiskUse: true
})


// useful mongo commands

db.zendeskUser.find({"_id": {$lt: 1}})

db.zendeskTicket.createIndex({operationId: 1})
db.zendeskComment.createIndex({operationId: 1})
db.zendeskUser.createIndex({operationId: 1})

db.zendeskMapping.createIndex({zendeskId: 1})
db.zendeskMapping.getIndexes()


// db.zendeskTenantImport.deleteMany({})
// db.zendeskOrganization.deleteMany({})
// db.zendeskGroup.deleteMany({})
// db.zendeskTicketField.deleteMany({})
// db.zendeskTicketForm.deleteMany({})
// db.zendeskUserField.deleteMany({})
// db.zendeskUser.deleteMany({})
// db.zendeskTicket.deleteMany({})
// db.zendeskComment.deleteMany({})
// db.commentMap.deleteMany({})
// db.importLog.deleteMany({})
// db.zendeskMapping.deleteMany({})


db.importLog.deleteMany({})
db.commentMap.deleteMany({})
db.zendeskMapping.deleteMany({grispiId: {$nin: [/ts/, /discarded/]}})

db.importLog.find({})
db.importLog.find({type: 'ERROR'})
db.importLog.find({type: 'WARNING'})
db.zendeskMapping.find({grispiId: {$in: [/ts/, /discarded/]}})
db.zendeskMapping.find({})


db.importLog.find({resourceName: 'deleted_user'}).count()

//
db.importLog.find({resourceName: 'ticket', type: 'SUCCESS'}).count()
db.importLog.find({resourceName: 'ticket', type: 'ERROR'}).count()
db.importLog.find({resourceName: 'ticket', type: 'ERROR', message: {$in: [/Failed to convert argument /]}})
db.importLog.find({resourceName: 'ticket', type: 'ERROR', message: {$in: [/requested user with zendesk reference id/]}}).count()

db.zendeskMapping.find({}).count()
db.zendeskMapping.find({})
db.zendeskMapping.deleteMany({})

db.importLog.deleteMany({resourceName: {$in: ['ticket', 'comment']}})
db.zendeskMapping.deleteMany({resourceName: {$in: ['user', 'deleted_user', 'ticket', 'comment']}})

db.zendeskTicket.find({}).limit(5)

db.commentMap.count()

db.importLog.count()
db.zendeskOrganization.count()
db.zendeskGroup.count()
db.zendeskUser.count()
db.zendeskUser.find({active: true}).count()
db.zendeskUser.find({active: false}).count()
db.zendeskTicket.count()

db.zendeskTicket.aggregate(
    {
        $lookup: {from: "zendeskMapping", localField: "_id", foreignField: "zendeskId", as: "zendeskMapping"}
    },
    { $unwind: "$item" },
    {
        $project: {
            "_id": 1,
            "status": 1,
            "grispiId": "$item.grispiId"
        }
    })
// db.zendeskTicket.find({brandId: 360002498720, operationId: "66a40cc1-3602-4f58-b115-943f1f5754d7"}, {"status": 1})

db.zendeskTicket.find({commentCount: {$gt: 1}}).count()
db.zendeskTicketField.count()
db.zendeskTicket.aggregate([
    { $match: { commentCount: {$gt: 1}} },
    {$group: { _id: null, total: {$sum: "$commentCount"}}}
] )
db.zendeskComment.find({}).count()
db.commentMap.find({waiting: true}).count()
db.commentMap.find({requested: true}).count()
db.commentMap.find({requested: false}).count()
db.commentMap.find({fetched: true}).count()
db.commentMap.count()

db.zendeskMapping.find({resourceName: 'user'}).sort({grispiId: 1})
db.zendeskMapping.count()

db.zendeskUser.count()
db.zendeskUser.find({organizationId: {$ne: null}}).count()
db.zendeskUser.find({organizationId: {$eq: null}})
db.zendeskUser.find({defaultGroupId: {$ne: null}})


db.importLog.find({type: 'SUCCESS'}).count()
db.importLog.find({type: 'ERROR',createdAt: {$gt: 1642680938000}})


// db.importLog.deleteMany({resourceName: "ticket_form", operationId: "8c124035-11be-4f7b-ade3-02d72336e459"})
db.importLog.find({resourceName: "ticket", type: 'ERROR', operationId: "9fe85137-1345-475f-b2b8-da9560dea992", message: {$nin: [/Unique constraint/, /For input string:/, /Subject is/]}})  //1780
db.importLog.deleteMany({resourceName: "comment"})
db.importLog.find({resourceName: "comment", type: 'ERROR'})
db.importLog.find({resourceName: "comment", type: 'ERROR'}).count()
db.importLog.find({resourceName: "comment", type: 'SUCCESS'})
db.importLog.find({resourceName: "comment", type: 'WARNING'}).count()
db.importLog.find({resourceName: "comment"}).count()
// db.importLog.deleteMany({resourceName: "comment", operationId: "9fe85137-1345-475f-b2b8-da9560dea992"})
db.importLog.find({resourceName: 'user', createdAt: {$gt: 1642680938000}}).count()
db.importLog.find({resourceName: 'user', type: 'ERROR'})
db.importLog.find({resourceName: 'deleted_user', type: 'SUCCESS'}).count()
db.importLog.find({resourceName: 'deleted_user', type: 'ERROR'}).count()
db.importLog.find({resourceName: 'user', type: 'SUCCESS'})
db.importLog.find({resourceName: 'user', type: 'WARNING'}).count()

db.importLog.find({resourceName: 'ticket'}).count()
db.importLog.find({resourceName: 'ticket', type: 'ERROR'}).count()
db.importLog.find({resourceName: 'ticket', type: 'SUCCESS'}).count()
db.importLog.find({resourceName: 'ticket', type: 'WARNING'}).count()



// AGGREGATION EXAMPLES

db.zendeskUser.aggregate([
    {$match: {_id: 362415139339}},
    {$lookup: {from: "zendeskGroupMembership", localField: "_id", foreignField: "userId", as: "groups"}},
    {$lookup: {
            from: "zendeskMapping", localField: "$groups.groupId", foreignField: "zendeskId",
            let: {order_item: "$groups.groupId"},
            pipeline: [
                { $match: { $expr: { $and: [{ $eq: [ "$stock_item",  "$$order_item" ] }]}}},
                { $project: { stock_item: 0, _id: 0 } }
            ],
            as: "grispiMappings"}},
    // { $unwind: "$item" },
    {
        $project: {
            "_id": 1,
            "name": 1,
            "groupIds": "$groups.groupId"
        }
    }
])

db.zendeskComment.aggregate([
    // {$match: {authorId: 362415139339}},
    {$match: {"operationId": {$eq: "9fe85137-1345-475f-b2b8-da9560dea992"}}},
    {$group: {_id: "$ticketId", comments: { $push: "$$ROOT" }}}
])
db.zendeskTicket.aggregate([
    {
        $lookup: {
            from: "zendeskComment",
            as: "comments",
            localField: "_id",
            foreignField: "ticketId"
        }
    },
    {
        $match: {
            operationId: "9fe85137-1345-475f-b2b8-da9560dea992",
            commentCount: {$gt: 1}
        }
    }
])

db.zendeskTicket.aggregate([
    {$match: {_id: {$in: [4137, 4162]}, commentCount: {$gt: 1}}},
    {$lookup: {from: "zendeskComment", as: "comments", localField: "_id", foreignField: "ticketId"}},
    {$lookup: {from: "zendeskMapping", as: "ticketMapping", localField: "_id", foreignField: "zendeskId"}},
    // {$lookup: {from: "zendeskMapping", as: "grispiUserId", localField: "_id", foreignField: "zendeskId"}},
    {$project: {_id: 1, comments: "$comments", ticketKey: "$ticketMapping.grispiId"}}
])

db.zendeskTicket.aggregate([
    {$match: {operationId: "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d", commentCount: {$gt: 1}}},
    {$lookup: {from: "zendeskComment", as: "zendeskComments", localField: "_id", foreignField: "ticketId"}},
    {$lookup: {from: "zendeskMapping", as: "ticketKeyMapping", localField: "_id", foreignField: "zendeskId"}},
    {$unwind: "$ticketKeyMapping"}
])

db.zendeskComment.find({operationId: "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"})




