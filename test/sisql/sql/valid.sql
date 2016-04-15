--@name:simple1
SELECT * FROM test
--@name:simple2
    SELECT * FROM test;
--@name: simple3
 SELECT * FROM test
-- @name: simple4


SELECT *
    FROM test


-- @name:simple5
      SELECT
*
    FROM
         test



--      @name:    simple6
SELECT *
    FROM test

-- @name:with-comments
-- this is a comment
SELECT *
-- table
FROM test
-- conditions
WHERE key=?

  -- @name:with-metadata
--this is a comment
--@doc: "Query docstring"
--@version: 12
 -- a comment between metadata
--@is-a-select?: true
-- @tag::some-tag


--      @long-doc:   "This is a \"test\" query"
SELECT true;

--@name:with-multiline-metadata
--@long-doc: "This is a \
multi\
line\
comment"
SELECT true

--@name:with-invalid-metadata-1
---@doc: "This is treated just as a comment"
SELECT true

--@name:with-invalid-metadata-2
--@d oc: "This is treated just as a comment"
SELECT true

--@name:bad
SELECT 1
--@name invalid name
--it should be attached to the previous query
SELECT 2
